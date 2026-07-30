package com.jnu.ticketapi.api.event.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.jnu.ticketdomain.domains.events.domain.Sector;
import com.jnu.ticketdomain.domains.registration.domain.Registration;
import com.jnu.ticketinfrastructure.model.StockReservationResult;
import com.jnu.ticketinfrastructure.service.WaitingQueueService;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class RegistrationAdmissionCoordinator {

    private final RegistrationResultPersistenceService registrationResultPersistenceService;
    private final Map<Long, EventAdmissionState> eventStates = new ConcurrentHashMap<>();

    @Autowired(required = false)
    private WaitingQueueService waitingQueueService;

    public RegistrationAdmissionCoordinator(
            RegistrationResultPersistenceService registrationResultPersistenceService) {
        this.registrationResultPersistenceService = registrationResultPersistenceService;
    }

    public StockReservationResult admit(
            Registration registration, Long userId, Sector sector, Long eventId)
            throws JsonProcessingException {
        EventAdmissionState state = state(eventId);
        Throwable redisFailure = null;
        Lock readLock = state.lock.readLock();
        readLock.lock();
        try {
            if (state.mode == AdmissionMode.DB_FALLBACK || waitingQueueService == null) {
                return persistWithDatabaseFallback(
                        registration, userId, sector.getId(), eventId);
            }

            try {
                StockReservationResult reservation =
                        waitingQueueService.reserveAndRegisterQueue(
                                waitingQueueService.eventStreamKey(eventId),
                                registration,
                                userId,
                                sector,
                                eventId);
                if (reservation.isUnavailable()) {
                    redisFailure =
                            new IllegalStateException(
                                    "Redis admission state is incomplete. eventId=" + eventId);
                } else if (!reservation.isReserved()) {
                    return reservation;
                } else {
                    return registrationResultPersistenceService.persistRedisReservation(
                            registration,
                            userId,
                            sector.getId(),
                            eventId,
                            reservation,
                            System.currentTimeMillis());
                }
            } catch (DataAccessException exception) {
                redisFailure = exception;
            }
        } finally {
            readLock.unlock();
        }

        activateDatabaseFallback(eventId, redisFailure);
        readLock.lock();
        try {
            return persistWithDatabaseFallback(registration, userId, sector.getId(), eventId);
        } finally {
            readLock.unlock();
        }
    }

    public boolean isDatabaseFallback(Long eventId) {
        return state(eventId).mode == AdmissionMode.DB_FALLBACK;
    }

    public void activateDatabaseFallback(Long eventId, Throwable cause) {
        EventAdmissionState state = state(eventId);
        Lock writeLock = state.lock.writeLock();
        writeLock.lock();
        try {
            if (state.mode != AdmissionMode.DB_FALLBACK) {
                state.mode = AdmissionMode.DB_FALLBACK;
                if (cause == null) {
                    log.warn("Registration admission switched to DB fallback. eventId: {}", eventId);
                } else {
                    log.warn(
                            "Registration admission switched to DB fallback. eventId: {}, cause: {}",
                            eventId,
                            cause.toString());
                }
            }
        } finally {
            writeLock.unlock();
        }
    }

    void activateRedis(Long eventId) {
        EventAdmissionState state = state(eventId);
        Lock writeLock = state.lock.writeLock();
        writeLock.lock();
        try {
            state.mode = AdmissionMode.REDIS;
        } finally {
            writeLock.unlock();
        }
    }

    ReentrantReadWriteLock.WriteLock recoveryLock(Long eventId) {
        return state(eventId).lock.writeLock();
    }

    private StockReservationResult persistWithDatabaseFallback(
            Registration registration, Long userId, Long sectorId, Long eventId) {
        return registrationResultPersistenceService.persistWithDatabaseFallback(
                registration, userId, sectorId, eventId, System.currentTimeMillis());
    }

    private EventAdmissionState state(Long eventId) {
        return eventStates.computeIfAbsent(eventId, ignored -> new EventAdmissionState());
    }

    private enum AdmissionMode {
        REDIS,
        DB_FALLBACK
    }

    private static final class EventAdmissionState {
        private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);
        private volatile AdmissionMode mode = AdmissionMode.REDIS;
    }
}
