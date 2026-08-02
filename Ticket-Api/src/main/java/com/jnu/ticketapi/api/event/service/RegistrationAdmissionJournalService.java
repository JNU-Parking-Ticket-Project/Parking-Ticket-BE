package com.jnu.ticketapi.api.event.service;


import com.jnu.ticketdomain.domains.events.adaptor.EventAdaptor;
import com.jnu.ticketdomain.domains.events.domain.Event;
import com.jnu.ticketdomain.domains.events.domain.EventAdmissionMode;
import com.jnu.ticketdomain.domains.events.domain.EventStatus;
import com.jnu.ticketdomain.domains.events.exception.NotOpenEventStatusException;
import com.jnu.ticketdomain.domains.events.exception.NotPublishEventException;
import com.jnu.ticketdomain.domains.registration.adaptor.RegistrationAdaptor;
import com.jnu.ticketdomain.domains.registration.adaptor.RegistrationAdmissionJournalAdaptor;
import com.jnu.ticketdomain.domains.registration.domain.Registration;
import com.jnu.ticketdomain.domains.registration.domain.RegistrationAdmissionJournal;
import com.jnu.ticketdomain.domains.registration.exception.AlreadyExistRegistrationException;
import com.jnu.ticketdomain.domains.user.adaptor.UserAdaptor;
import com.jnu.ticketinfrastructure.model.RegistrationPayloadConverter;
import com.jnu.ticketinfrastructure.model.StockReservationResult;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RegistrationAdmissionJournalService {
    private final RegistrationAdmissionJournalAdaptor admissionJournalAdaptor;
    private final RegistrationResultPersistenceService registrationResultPersistenceService;
    private final EventAdaptor eventAdaptor;
    private final RegistrationAdaptor registrationAdaptor;
    private final UserAdaptor userAdaptor;

    @Value("${registration.admission.materialization-grace-ms:5000}")
    private long materializationGraceMillis;

    private long materializationCursor;
    private long materializationUpperBound;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AdmissionAttempt openJournal(
            Registration registration,
            Long userId,
            Long sectorId,
            Long eventId,
            String registrationPayload,
            long receivedAt) {
        Event event = eventAdaptor.findByIdForAdmissionRead(eventId);
        userAdaptor.findByIdForUpdate(userId);
        String frozenRegistrationPayload =
                registrationAdaptor
                        .findTemporaryByEmailAndEventIdForUpdate(registration.getEmail(), eventId)
                        .map(
                                temporaryRegistration -> {
                                    registration.setId(temporaryRegistration.getId());
                                    registration.setCreatedAt(temporaryRegistration.getCreatedAt());
                                    return RegistrationPayloadConverter.toJson(registration);
                                })
                        .orElse(registrationPayload);
        Optional<RegistrationAdmissionJournal> existingJournal =
                admissionJournalAdaptor.findByEventIdAndEmail(eventId, registration.getEmail());
        if (existingJournal.isPresent()) {
            return existingAttempt(
                    existingJournal.get(),
                    event.getAdmissionMode(),
                    userId,
                    sectorId,
                    registration.getEmail(),
                    frozenRegistrationPayload);
        }
        if (event.getEventStatus() != EventStatus.OPEN) {
            throw NotOpenEventStatusException.EXCEPTION;
        }
        if (Boolean.FALSE.equals(event.getPublish())) {
            throw NotPublishEventException.EXCEPTION;
        }
        RegistrationAdmissionJournal journal =
                admissionJournalAdaptor.saveAndFlush(
                        RegistrationAdmissionJournal.received(
                                eventId,
                                sectorId,
                                userId,
                                registration.getEmail(),
                                event.getAdmissionEpoch(),
                                frozenRegistrationPayload,
                                receivedAt));
        return new AdmissionAttempt(journal, event.getAdmissionMode(), false);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<Registration> lockForTemporarySave(Long userId, Long eventId, String email) {
        userAdaptor.findByIdForUpdate(userId);
        Optional<Registration> temporaryRegistration =
                registrationAdaptor.findTemporaryByEmailAndEventIdForUpdate(email, eventId);
        if (admissionJournalAdaptor.existsByEventIdAndEmail(eventId, email)) {
            throw AlreadyExistRegistrationException.EXCEPTION;
        }
        return temporaryRegistration;
    }

    @Transactional(readOnly = true)
    public Optional<ExistingAdmission> findExistingAdmission(
            Registration registration, Long userId, Long sectorId, Long eventId) {
        return admissionJournalAdaptor
                .findByEventIdAndEmail(eventId, registration.getEmail())
                .filter(
                        journal ->
                                Objects.equals(journal.getUserId(), userId)
                                        && Objects.equals(journal.getSectorId(), sectorId)
                                        && RegistrationPayloadConverter.hasSameBusinessFields(
                                                journal.getRegistrationPayload(), registration))
                .map(journal -> new ExistingAdmission(journal.isDecided()));
    }

    @Transactional(readOnly = true)
    public AdmissionAttempt findExisting(
            Long eventId, String email, Long userId, Long sectorId, String registrationPayload) {
        Event event = eventAdaptor.findByIdForAdmissionRead(eventId);
        RegistrationAdmissionJournal journal =
                admissionJournalAdaptor
                        .findByEventIdAndEmail(eventId, email)
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "동시 생성된 신청 저널을 찾을 수 없습니다. eventId="
                                                        + eventId
                                                        + ", email="
                                                        + email));
        return existingAttempt(
                journal, event.getAdmissionMode(), userId, sectorId, email, registrationPayload);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public StockReservationResult rejectRedisDecision(
            Long journalId, StockReservationResult reservation, long decidedAt) {
        RegistrationAdmissionJournal journal = admissionJournalAdaptor.findByIdForUpdate(journalId);
        if (journal.isRejected() || journal.isDecided()) {
            return toResult(journal);
        }
        Event event = eventAdaptor.findByIdForAdmissionRead(journal.getEventId());
        if (!event.isRedisAdmission(journal.getAdmissionEpoch())) {
            throw new AdmissionEpochChangedException(event.getId(), journalId);
        }
        journal.reject(reservation.getReason(), reservation.getRemainingAmount(), decidedAt);
        return reservation;
    }

    public void materializeMissingRegistrations(Long eventId) {
        admissionJournalAdaptor
                .findDecidedByEventId(eventId, materializationCutoff())
                .forEach(this::materialize);
    }

    public void persistReceivedInDatabaseFallback(Long eventId, long throughJournalId) {
        admissionJournalAdaptor
                .findReceivedThrough(eventId, throughJournalId)
                .forEach(
                        journal ->
                                registrationResultPersistenceService
                                        .persistJournalWithDatabaseFallback(
                                                journal.getId(), System.currentTimeMillis()));
    }

    public void recoverReceivedInDatabaseFallback(Long eventId) {
        for (RegistrationAdmissionJournal journal :
                admissionJournalAdaptor.findReceivedThrough(eventId, Long.MAX_VALUE)) {
            if (!recoverReceived(journal)) {
                return;
            }
        }
    }

    public synchronized void materializeMissingRegistrations() {
        long cutoff = materializationCutoff();
        if (materializationUpperBound == 0L || materializationCursor >= materializationUpperBound) {
            materializationCursor = 0L;
            materializationUpperBound = admissionJournalAdaptor.findMaxDecidedId(cutoff);
        }
        if (materializationUpperBound == 0L) {
            return;
        }
        List<RegistrationAdmissionJournal> journals =
                admissionJournalAdaptor.findDecidedBatch(
                        cutoff, materializationCursor, materializationUpperBound);
        if (journals.isEmpty()) {
            materializationCursor = materializationUpperBound;
            return;
        }
        journals.forEach(this::materialize);
        journals.stream()
                .map(RegistrationAdmissionJournal::getId)
                .max(Long::compareTo)
                .ifPresent(id -> materializationCursor = id);
    }

    public StockReservationResult toResult(RegistrationAdmissionJournal journal) {
        if (journal.isDecided()) {
            return StockReservationResult.reserved(
                    journal.getPosition(),
                    journal.getResultStatus(),
                    journal.getSequence(),
                    journal.getRemainingAmount());
        }
        if (!journal.isRejected()) {
            throw new IllegalStateException("아직 확정되지 않은 신청입니다. journalId=" + journal.getId());
        }
        return switch (journal.getDecisionReason()) {
            case "DUPLICATE" -> StockReservationResult.duplicate(journal.getRemainingAmount());
            case "NO_STOCK" -> StockReservationResult.noStock(journal.getRemainingAmount());
            case "CLOSED" -> StockReservationResult.closed(journal.getRemainingAmount());
            default -> StockReservationResult.unavailable(journal.getRemainingAmount());
        };
    }

    public int findMaxDecidedPosition(Long sectorId) {
        return admissionJournalAdaptor.findMaxPositionBySectorId(sectorId);
    }

    private void materialize(RegistrationAdmissionJournal journal) {
        try {
            registrationResultPersistenceService.materializeConfirmedJournal(journal.getId());
        } catch (RuntimeException exception) {
            log.warn(
                    "Confirmed registration journal materialization failed. journalId: {}",
                    journal.getId(),
                    exception);
        }
    }

    private boolean recoverReceived(RegistrationAdmissionJournal journal) {
        try {
            registrationResultPersistenceService.persistJournalWithDatabaseFallback(
                    journal.getId(), System.currentTimeMillis());
            return true;
        } catch (RuntimeException exception) {
            log.warn(
                    "Received registration journal DB fallback failed. journalId: {}",
                    journal.getId(),
                    exception);
            return false;
        }
    }

    private AdmissionAttempt existingAttempt(
            RegistrationAdmissionJournal journal,
            EventAdmissionMode admissionMode,
            Long userId,
            Long sectorId,
            String email,
            String registrationPayload) {
        if (!Objects.equals(journal.getUserId(), userId)
                || !Objects.equals(journal.getSectorId(), sectorId)
                || !Objects.equals(journal.getEmail(), email)
                || !RegistrationPayloadConverter.hasSameBusinessFields(
                        journal.getRegistrationPayload(), registrationPayload)) {
            throw AlreadyExistRegistrationException.EXCEPTION;
        }
        return new AdmissionAttempt(journal, admissionMode, true);
    }

    private long materializationCutoff() {
        return System.currentTimeMillis() - Math.max(0L, materializationGraceMillis);
    }

    public record AdmissionAttempt(
            RegistrationAdmissionJournal journal,
            EventAdmissionMode admissionMode,
            boolean existing) {
        public boolean hasResult() {
            return journal.isDecided() || journal.isRejected();
        }
    }

    public record ExistingAdmission(boolean accepted) {}
}
