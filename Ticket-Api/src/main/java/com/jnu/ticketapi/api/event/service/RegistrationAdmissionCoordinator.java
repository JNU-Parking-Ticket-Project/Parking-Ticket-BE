package com.jnu.ticketapi.api.event.service;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.jnu.ticketdomain.domains.events.domain.Sector;
import com.jnu.ticketdomain.domains.registration.domain.Registration;
import com.jnu.ticketinfrastructure.admission.RegistrationAdmissionFallbackGateway;
import com.jnu.ticketinfrastructure.model.StockReservationResult;
import com.jnu.ticketinfrastructure.service.WaitingQueueService;
import com.jnu.ticketinfrastructure.stream.RedisStreamConsumerManager;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class RegistrationAdmissionCoordinator implements RegistrationAdmissionFallbackGateway {

    private final RegistrationResultPersistenceService registrationResultPersistenceService;
    private final Map<Long, EventAdmissionState> eventStates = new ConcurrentHashMap<>();

    @Autowired(required = false)
    private WaitingQueueService waitingQueueService;

    @Autowired(required = false)
    private RedisStreamConsumerManager streamConsumerManager;

    @Value("${redis.admission.recovery-drain-timeout-ms:30000}")
    private long recoveryDrainTimeoutMillis = 30_000L;

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
                return persistWithDatabaseFallback(registration, userId, sector.getId(), eventId);
            }

            StockReservationResult reservation = null;
            try {
                reservation =
                        waitingQueueService.reserveAndRegisterQueue(
                                waitingQueueService.eventStreamKey(eventId),
                                registration,
                                userId,
                                sector,
                                eventId);
            } catch (DataAccessException exception) {
                redisFailure = exception;
            }

            if (redisFailure == null) {
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

    public Set<Long> fallbackEventIds() {
        return eventStates.entrySet().stream()
                .filter(entry -> entry.getValue().mode == AdmissionMode.DB_FALLBACK)
                .map(Map.Entry::getKey)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    @Override
    public void activateDatabaseFallback(Long eventId, Throwable cause) {
        EventAdmissionState state = state(eventId);
        Lock writeLock = state.lock.writeLock();
        writeLock.lock();
        try {
            if (state.mode != AdmissionMode.DB_FALLBACK) {
                state.mode = AdmissionMode.DB_FALLBACK;
                if (cause == null) {
                    log.warn(
                            "Registration admission switched to DB fallback. eventId: {}", eventId);
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

    public boolean recover(Long eventId) {
        EventAdmissionState state = state(eventId);
        if (!state.recoveryRunning.compareAndSet(false, true)) {
            return false;
        }
        try {
            if (waitingQueueService == null || !waitingQueueService.isAvailable()) {
                return false;
            }
            if (!drainExistingStream(eventId)) {
                return false;
            }

            Lock writeLock = state.lock.writeLock();
            writeLock.lock();
            try {
                EventStockRecoverySnapshot snapshot =
                        registrationResultPersistenceService.prepareRecoverySnapshot(eventId);
                if (!waitingQueueService.rebuildEventStock(
                        eventId, snapshot.sectors(), snapshot.reservedEmails())) {
                    return false;
                }
                state.mode = AdmissionMode.REDIS;
            } finally {
                writeLock.unlock();
            }

            if (streamConsumerManager != null) {
                streamConsumerManager.start(eventId);
            }
            log.info("Redis admission state recovered from DB. eventId: {}", eventId);
            return true;
        } catch (RuntimeException exception) {
            log.warn(
                    "Redis admission recovery failed; DB fallback remains active. eventId: {}",
                    eventId,
                    exception);
            return false;
        } finally {
            state.recoveryRunning.set(false);
        }
    }

    private boolean drainExistingStream(Long eventId) {
        if (streamConsumerManager == null) {
            return true;
        }
        Duration timeout = Duration.ofMillis(Math.max(1L, recoveryDrainTimeoutMillis));
        return streamConsumerManager.requestDrain(eventId)
                && streamConsumerManager.awaitDrainCompletion(eventId, timeout);
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
        private final AtomicBoolean recoveryRunning = new AtomicBoolean();
        private volatile AdmissionMode mode = AdmissionMode.REDIS;
    }
}
