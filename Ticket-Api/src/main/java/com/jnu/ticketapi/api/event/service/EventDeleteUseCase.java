package com.jnu.ticketapi.api.event.service;


import com.jnu.ticketcommon.annotation.UseCase;
import com.jnu.ticketdomain.common.domainEvent.Events;
import com.jnu.ticketdomain.domains.events.adaptor.EventAdaptor;
import com.jnu.ticketdomain.domains.events.adaptor.SectorAdaptor;
import com.jnu.ticketdomain.domains.events.domain.Event;
import com.jnu.ticketdomain.domains.events.domain.EventStatus;
import com.jnu.ticketdomain.domains.events.event.EventDeletedEvent;
import com.jnu.ticketdomain.domains.registration.adaptor.RegistrationAdaptor;
import com.jnu.ticketinfrastructure.service.WaitingQueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@UseCase
@RequiredArgsConstructor
public class EventDeleteUseCase {
    private final EventAdaptor eventAdaptor;

    @Autowired(required = false)
    private WaitingQueueService waitingQueueService;

    private final SectorAdaptor sectorAdaptor;
    private final RegistrationAdaptor registrationAdaptor;

    @Value("${ableRedis:true}")
    private boolean ableRedis;

    @Transactional
    public void deleteEvent(Long eventId) {
        Event event = eventAdaptor.findById(eventId);
        Events.raise(EventDeletedEvent.of(event));
        event.deleteEvent();
        event.updateStatus(EventStatus.CLOSED, null);
        deleteEventRedisStateAfterCommit(eventId);
        sectorAdaptor.deleteByEvent(eventId);
        registrationAdaptor.deleteByEvent(eventId);
    }

    private void deleteEventRedisStateAfterCommit(Long eventId) {
        if (!ableRedis || waitingQueueService == null) {
            return;
        }
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            deleteEventRedisState(eventId);
                        }
                    });
            return;
        }
        deleteEventRedisState(eventId);
    }

    private void deleteEventRedisState(Long eventId) {
        waitingQueueService.deleteEventStream(eventId);
        waitingQueueService.deleteEventStockKeys(eventId);
    }
}
