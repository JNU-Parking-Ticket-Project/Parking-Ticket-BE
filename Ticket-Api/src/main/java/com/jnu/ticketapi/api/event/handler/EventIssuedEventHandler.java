package com.jnu.ticketapi.api.event.handler;

import static com.jnu.ticketcommon.consts.TicketStatic.REDIS_EVENT_ISSUE_STORE;

import com.jnu.ticketdomain.common.domainEvent.Events;
import com.jnu.ticketdomain.domains.events.adaptor.SectorAdaptor;
import com.jnu.ticketdomain.domains.events.domain.Sector;
import com.jnu.ticketdomain.domains.registration.adaptor.RegistrationAdaptor;
import com.jnu.ticketdomain.domains.registration.domain.Registration;
import com.jnu.ticketdomain.domains.registration.event.RegistrationCreationEvent;
import com.jnu.ticketdomain.domains.user.adaptor.UserAdaptor;
import com.jnu.ticketdomain.domains.user.domain.User;
import com.jnu.ticketinfrastructure.domainEvent.EventIssuedEvent;
import com.jnu.ticketinfrastructure.model.ChatMessage;
import com.jnu.ticketinfrastructure.service.WaitingQueueService;
import java.util.Comparator;
import java.util.List;
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
    private final UserAdaptor userAdaptor;
    private final WaitingQueueService waitingQueueService;
    private final SectorAdaptor sectorAdaptor;

    @Async
    @TransactionalEventListener(
            classes = EventIssuedEvent.class,
            phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(EventIssuedEvent eventIssuedEvent) {
        processEventData(
                eventIssuedEvent.getCurrentUserId(),
                eventIssuedEvent.getEventId(),
                eventIssuedEvent.getSectorId());
        waitingQueueService.popQueue(REDIS_EVENT_ISSUE_STORE, 1, ChatMessage.class);
    }

    /**
     * 1차신청에 대한 유저의 신청 결과 상태 정보를 변경하는 로직 1차신청에 대한 유저 신청 결과 상태 정보를 메일 전송하는 이벤트를 발행한다.
     *
     * @param userId
     * @author : cookie, blackbean
     */
    public void processEventData(Long userId, Long eventId, Long sectorId) {
        User user = userAdaptor.findById(userId);
        List<Registration> registrations = registrationAdaptor.findByUserId(userId);
        Sector sector = sectorAdaptor.findById(sectorId);
        Registration registration =
                registrations.stream()
                        .filter(r -> r.getUser().getId().equals(userId))
                        .max(Comparator.comparing(Registration::getId))
                        .orElse(null);

        if (sector.isSectorCapacityRemaining()) {
            user.success();
        } else if (sector.isSectorReserveRemaining()) {

            Long waitingOrder =
                    waitingQueueService.getWaitingOrder(REDIS_EVENT_ISSUE_STORE, new ChatMessage(userId, eventId, sectorId));
            user.prepare(Integer.valueOf(waitingOrder.intValue()) + 1);
        } else {
            user.fail();
        }
        Events.raise(
                RegistrationCreationEvent.of(registration, user.getStatus(), user.getSequence()));
        sector.decreaseEventStock();
    }
}
