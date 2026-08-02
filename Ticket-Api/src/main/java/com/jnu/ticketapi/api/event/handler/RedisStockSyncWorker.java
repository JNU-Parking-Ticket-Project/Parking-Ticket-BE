package com.jnu.ticketapi.api.event.handler;


import com.jnu.ticketapi.api.event.service.RegistrationAdmissionCoordinator;
import com.jnu.ticketdomain.domains.events.adaptor.SectorAdaptor;
import com.jnu.ticketdomain.domains.events.domain.EventStatus;
import com.jnu.ticketdomain.domains.events.domain.Sector;
import com.jnu.ticketinfrastructure.service.WaitingQueueService;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnExpression("${ableRedis:true}")
@ConditionalOnProperty(
        value = "redis.stock-sync.enabled",
        havingValue = "true",
        matchIfMissing = true)
public class RedisStockSyncWorker {

    private final SectorAdaptor sectorAdaptor;
    private final RegistrationAdmissionCoordinator registrationAdmissionCoordinator;

    @Autowired(required = false)
    private WaitingQueueService waitingQueueService;

    @Scheduled(
            fixedDelayString = "${redis.stock-sync.fixed-delay-ms:10000}",
            initialDelayString = "${redis.stock-sync.initial-delay-ms:10000}")
    @Transactional(readOnly = true)
    public void syncRemainingStock() {
        if (waitingQueueService == null) {
            return;
        }
        sectorAdaptor.findAllByEventStatusAndPublishAndIsDeleted().forEach(this::syncSector);
    }

    void syncSector(Sector sector) {
        Long eventId = sector.getEvent().getId();
        if (sector.getEvent().getEventStatus() != EventStatus.OPEN
                || registrationAdmissionCoordinator.isRedisAdmissionUnavailable(eventId)) {
            return;
        }
        try {
            Optional<Integer> redisRemaining =
                    waitingQueueService.findRemainingStock(eventId, sector.getId());
            Optional<Integer> redisPosition =
                    waitingQueueService.findAssignedPosition(eventId, sector.getId());
            Integer dbRemaining = sector.getRemainingAmount();
            int issueAmount = sector.getIssueAmount();
            int decidedPosition =
                    registrationAdmissionCoordinator.findMaxDecidedPosition(sector.getId());
            if (redisRemaining.isEmpty()
                    || redisPosition.isEmpty()
                    || dbRemaining == null
                    || redisRemaining.get() < 0
                    || redisRemaining.get() > issueAmount
                    || redisPosition.get() < decidedPosition
                    || redisPosition.get() > issueAmount
                    || redisRemaining.get() + redisPosition.get() != issueAmount
                    || redisRemaining.get() > dbRemaining) {
                registrationAdmissionCoordinator.activateDatabaseFallback(
                        eventId,
                        new IllegalStateException(
                                "Redis admission checkpoint is unsafe. sectorId="
                                        + sector.getId()
                                        + ", redisRemaining="
                                        + redisRemaining.orElse(null)
                                        + ", redisPosition="
                                        + redisPosition.orElse(null)
                                        + ", decidedPosition="
                                        + decidedPosition
                                        + ", dbRemaining="
                                        + dbRemaining));
            }
        } catch (RuntimeException exception) {
            registrationAdmissionCoordinator.activateDatabaseFallback(eventId, exception);
        }
    }
}
