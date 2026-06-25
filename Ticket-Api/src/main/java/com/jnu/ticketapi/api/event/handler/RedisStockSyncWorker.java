package com.jnu.ticketapi.api.event.handler;


import com.jnu.ticketdomain.domains.events.adaptor.SectorAdaptor;
import com.jnu.ticketdomain.domains.events.domain.Sector;
import com.jnu.ticketinfrastructure.service.WaitingQueueService;
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

    @Autowired(required = false)
    private WaitingQueueService waitingQueueService;

    @Scheduled(
            fixedDelayString = "${redis.stock-sync.fixed-delay-ms:10000}",
            initialDelayString = "${redis.stock-sync.initial-delay-ms:10000}")
    @Transactional
    public void syncRemainingStock() {
        if (waitingQueueService == null) {
            return;
        }
        sectorAdaptor.findAllByEventStatusAndPublishAndIsDeleted().forEach(this::syncSector);
    }

    void syncSector(Sector sector) {
        waitingQueueService
                .findRemainingStock(sector.getEvent().getId(), sector.getId())
                .ifPresent(
                        remainingAmount -> {
                            sector.syncRemainingAmount(remainingAmount);
                            sectorAdaptor.save(sector);
                            log.info(
                                    "Synced Redis stock to DB. eventId: {}, sectorId: {}, remaining: {}",
                                    sector.getEvent().getId(),
                                    sector.getId(),
                                    remainingAmount);
                        });
    }
}
