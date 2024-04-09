package com.jnu.ticketapi.api.event.handler;

import static com.jnu.ticketcommon.consts.TicketStatic.REDIS_EVENT_ISSUE_STORE;

import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final ObjectMapper objectMapper;

    @Async
    @TransactionalEventListener(
            classes = EventIssuedEvent.class,
            phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(EventIssuedEvent eventIssuedEvent) {
        processEventData(eventIssuedEvent);
        waitingQueueService.popQueue(REDIS_EVENT_ISSUE_STORE, 1, ChatMessage.class);
    }

    /**
     * 1차신청에 대한 유저의 신청 결과 상태 정보를 변경하는 로직 1차신청에 대한 유저 신청 결과 상태 정보를 메일 전송하는 이벤트를 발행한다.
     *
     * @author : cookie, blackbean
     */
    public void processEventData(EventIssuedEvent event) {
        User user = userAdaptor.findById(event.getCurrentUserId());
        Sector sector = sectorAdaptor.findById(event.getSectorId());
        Registration registration = event.getRegistration();

        saveRegistration(sector, user, registration);
        reflectUserState(event, sector, user);

        Events.raise(
                RegistrationCreationEvent.of(
                        event.getRegistration(), user.getStatus(), user.getSequence()));
        sector.decreaseEventStock();
    }

    private void reflectUserState(EventIssuedEvent event, Sector sector, User user) {
        if (sector.isSectorCapacityRemaining()) {
            user.success();
        } else if (sector.isSectorReserveRemaining()) {
            try {
                String registrationString =
                        waitingQueueService.convertRegistrationJSON(event.getRegistration());
                Long waitingOrder =
                        waitingQueueService.getWaitingOrder( // getWaitingOrder는 0번부터 시작
                                REDIS_EVENT_ISSUE_STORE,
                                new ChatMessage(
                                        registrationString,
                                        event.getCurrentUserId(),
                                        event.getSectorId()));
                user.prepare(Integer.valueOf(waitingOrder.intValue()) + 1);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            user.fail();
        }
    }

    private void saveRegistration(Sector sector, User user, Registration registration) {
        registration.setSector(sector);
        registration.setUser(user);
        registrationAdaptor.saveAndFlush(registration);
    }
}
