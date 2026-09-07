package com.jnu.ticketapi.api.event.service;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.jnu.ticketdomain.domains.events.domain.EventAdmissionMode;
import com.jnu.ticketdomain.domains.events.domain.Sector;
import com.jnu.ticketdomain.domains.events.exception.RedisStockUnavailableException;
import com.jnu.ticketdomain.domains.registration.domain.Registration;
import com.jnu.ticketinfrastructure.model.StockReservationResult;
import com.jnu.ticketinfrastructure.service.WaitingQueueService;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RegistrationAdmissionCoordinator {
    private static final Duration FALLBACK_REDIS_FENCE_TTL = Duration.ofDays(30);

    private final RegistrationResultPersistenceService registrationResultPersistenceService;
    private final RegistrationAdmissionJournalService registrationAdmissionJournalService;
    private final EventAdmissionControlService eventAdmissionControlService;

    @Autowired(required = false)
    private WaitingQueueService waitingQueueService;

    public StockReservationResult admit(
            Registration registration, Long userId, Sector sector, Long eventId)
            throws JsonProcessingException {
        if (waitingQueueService == null) {
            throw RedisStockUnavailableException.EXCEPTION;
        }

        String registrationPayload = waitingQueueService.convertRegistrationJSON(registration);
        RegistrationAdmissionJournalService.AdmissionAttempt attempt =
                openJournal(registration, userId, sector.getId(), eventId, registrationPayload);
        if (attempt.hasResult()) {
            return registrationAdmissionJournalService.toResult(attempt.journal());
        }
        if (attempt.admissionMode() == EventAdmissionMode.DB_FALLBACK) {
            persistReceivedPrefix(eventId, attempt.journal().getId());
            return persistWithDatabaseFallback(attempt.journal().getId());
        }

        StockReservationResult reservation = null;
        RuntimeException redisFailure = null;
        // Lua는 journalId 기준 멱등이다. 응답 유실 가능성을 먼저 한 번 재확인한 뒤
        // Redis가 실제로 사용할 수 없을 때만 DB 순번으로 전환한다.
        for (int redisAttempt = 0; redisAttempt < 2; redisAttempt++) {
            try {
                reservation =
                        waitingQueueService.reserveAndRegisterQueue(
                                waitingQueueService.eventStreamKey(eventId),
                                attempt.journal().getRegistrationPayload(),
                                attempt.journal().getEmail(),
                                attempt.journal().getUserId(),
                                sector,
                                eventId,
                                attempt.journal().getId(),
                                attempt.journal().getAdmissionEpoch());
                break;
            } catch (RuntimeException exception) {
                redisFailure = exception;
            }
        }
        if (reservation == null) {
            return fallBackCurrentRequest(eventId, attempt.journal().getId(), redisFailure);
        }

        if (reservation.isUnavailable()) {
            return fallBackCurrentRequest(
                    eventId,
                    attempt.journal().getId(),
                    new IllegalStateException(
                            "Redis admission state is incomplete. eventId=" + eventId));
        }
        if (reservation.isClosed() && eventAdmissionControlService.isOpenForAdmission(eventId)) {
            return fallBackCurrentRequest(
                    eventId,
                    attempt.journal().getId(),
                    new IllegalStateException(
                            "Redis admission was fenced while the DB event remained open. eventId="
                                    + eventId));
        }
        if (!reservation.isReserved()) {
            try {
                return registrationAdmissionJournalService.rejectRedisDecision(
                        attempt.journal().getId(), reservation, System.currentTimeMillis());
            } catch (AdmissionEpochChangedException exception) {
                return fallBackCurrentRequest(eventId, attempt.journal().getId(), exception);
            }
        }

        try {
            return registrationResultPersistenceService.confirmRedisDecision(
                    attempt.journal().getId(), reservation, System.currentTimeMillis());
        } catch (AdmissionEpochChangedException
                | DataIntegrityViolationException
                | RedisAdmissionInvariantException exception) {
            return fallBackCurrentRequest(eventId, attempt.journal().getId(), exception);
        }
    }

    public boolean isDatabaseFallback(Long eventId) {
        return eventAdmissionControlService.isDatabaseFallback(eventId);
    }

    public Optional<RegistrationAdmissionJournalService.ExistingAdmission> findExistingAdmission(
            Registration registration, Long userId, Sector sector, Long eventId) {
        return registrationAdmissionJournalService.findExistingAdmission(
                registration, userId, sector.getId(), eventId);
    }

    public boolean isRedisAdmissionUnavailable(Long eventId) {
        return isDatabaseFallback(eventId);
    }

    public Set<Long> recoveryEventIds() {
        return eventAdmissionControlService.fallbackEventIds();
    }

    public void activateDatabaseFallback(Long eventId, Throwable cause) {
        if (waitingQueueService != null) {
            try {
                waitingQueueService.markEventStockClosed(eventId, FALLBACK_REDIS_FENCE_TTL);
            } catch (RuntimeException fenceFailure) {
                log.warn(
                        "Redis fallback fence could not be written; continuing with persistent DB"
                                + " fallback. eventId: {}",
                        eventId,
                        fenceFailure);
            }
        }
        eventAdmissionControlService.activateDatabaseFallback(eventId, cause);
    }

    public void restoreOpenEventAdmission(Long eventId) {
        if (isDatabaseFallback(eventId)) {
            recover(eventId);
            return;
        }
        try {
            if (waitingQueueService != null
                    && waitingQueueService.isAvailable()
                    && waitingQueueService.isEventStockInitialized(eventId)) {
                recover(eventId);
                return;
            }
        } catch (RuntimeException exception) {
            activateDatabaseFallback(eventId, exception);
            recover(eventId);
            return;
        }
        activateDatabaseFallback(
                eventId,
                new IllegalStateException("Redis admission is unavailable during startup"));
        recover(eventId);
    }

    public boolean recover(Long eventId) {
        try {
            if (isDatabaseFallback(eventId)) {
                registrationAdmissionJournalService.recoverReceivedInDatabaseFallback(eventId);
            }
            registrationAdmissionJournalService.materializeMissingRegistrations(eventId);
            return true;
        } catch (RuntimeException exception) {
            log.warn(
                    "Admission journal reconciliation failed; DB fallback remains active. eventId:"
                            + " {}",
                    eventId,
                    exception);
            return false;
        }
    }

    public void reconcileConfirmedRegistrations() {
        registrationAdmissionJournalService.materializeMissingRegistrations();
    }

    public int findMaxDecidedPosition(Long sectorId) {
        return registrationAdmissionJournalService.findMaxDecidedPosition(sectorId);
    }

    private RegistrationAdmissionJournalService.AdmissionAttempt openJournal(
            Registration registration,
            Long userId,
            Long sectorId,
            Long eventId,
            String registrationPayload) {
        try {
            return registrationAdmissionJournalService.openJournal(
                    registration,
                    userId,
                    sectorId,
                    eventId,
                    registrationPayload,
                    System.currentTimeMillis());
        } catch (DataIntegrityViolationException conflict) {
            try {
                return registrationAdmissionJournalService.findExisting(
                        eventId, registration.getEmail(), userId, sectorId, registrationPayload);
            } catch (IllegalStateException noUniqueConflict) {
                conflict.addSuppressed(noUniqueConflict);
                throw conflict;
            }
        }
    }

    private StockReservationResult fallBackCurrentRequest(
            Long eventId, Long journalId, Throwable cause) {
        activateDatabaseFallback(eventId, cause);
        persistReceivedPrefix(eventId, journalId);
        return persistWithDatabaseFallback(journalId);
    }

    private void persistReceivedPrefix(Long eventId, Long throughJournalId) {
        DataIntegrityViolationException lastConflict = null;
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                registrationAdmissionJournalService.persistReceivedInDatabaseFallback(
                        eventId, throughJournalId);
                return;
            } catch (DataIntegrityViolationException conflict) {
                lastConflict = conflict;
            }
        }
        throw new IllegalStateException(
                "DB fallback 선행 순번 경합을 해결하지 못했습니다. eventId=" + eventId, lastConflict);
    }

    private StockReservationResult persistWithDatabaseFallback(Long journalId) {
        DataIntegrityViolationException lastConflict = null;
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                return registrationResultPersistenceService.persistJournalWithDatabaseFallback(
                        journalId, System.currentTimeMillis());
            } catch (DataIntegrityViolationException conflict) {
                lastConflict = conflict;
            }
        }
        throw new IllegalStateException(
                "DB fallback 순번 경합을 해결하지 못했습니다. journalId=" + journalId, lastConflict);
    }
}
