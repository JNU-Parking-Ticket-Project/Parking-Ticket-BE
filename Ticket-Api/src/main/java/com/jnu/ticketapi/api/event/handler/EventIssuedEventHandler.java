package com.jnu.ticketapi.api.event.handler;

import static com.jnu.ticketcommon.consts.TicketStatic.REDIS_EVENT_ISSUE_STORE;

import com.jnu.ticketdomain.domains.events.domain.Sector;
import com.jnu.ticketdomain.domains.registration.adaptor.RegistrationAdaptor;
import com.jnu.ticketdomain.domains.registration.domain.Registration;
import com.jnu.ticketdomain.domains.user.domain.User;
import com.jnu.ticketinfrastructure.domainEvent.EventIssuedEvent;
import com.jnu.ticketinfrastructure.model.ChatMessage;
import com.jnu.ticketinfrastructure.service.WaitingQueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventIssuedEventHandler {
    private final RegistrationAdaptor registrationAdaptor;
    private final WaitingQueueService waitingQueueService;

    @Async
    @TransactionalEventListener(
            classes = EventIssuedEvent.class,
            phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(EventIssuedEvent eventIssuedEvent) {
        processEventData(eventIssuedEvent.getCurrentUserId());
        waitingQueueService.popQueue(REDIS_EVENT_ISSUE_STORE, 1, ChatMessage.class);
    }

    private void processEventData(Long userId) {
        Registration registration = registrationAdaptor.findByUserId(userId);
        Sector sector = registration.getSector();
        User user = registration.getUser();

        if (sector.isSectorRemaining()) {
            user.success();
        } else if (sector.isSectorReserveRemaining()) {
            Long waitingOrder =
                    waitingQueueService.getWaitingOrder(REDIS_EVENT_ISSUE_STORE, userId);
            user.prepare(Integer.valueOf(waitingOrder.intValue()));
        } else {
            user.fail();
        }

        sector.decreaseEventStock();
    }
}
