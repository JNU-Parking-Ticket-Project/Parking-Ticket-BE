package com.jnu.ticketapi.api.event.handler;


import com.fasterxml.jackson.core.JsonProcessingException;
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
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
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
    private final DataSource dataSource;

    @Async
    @TransactionalEventListener(
            classes = EventIssuedEvent.class,
            phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(EventIssuedEvent eventIssuedEvent) {
        Sector sector = sectorAdaptor.findById(eventIssuedEvent.getSectorId());
        if (isConnectionAvailable()) {
            String key = eventIssuedEvent.getEventId().toString() + "-" + sector.getSectorNumber();
            ChatMessage message = (ChatMessage) waitingQueueService.getValue(key);

            if (message != null) {
                try {
                    Registration registration =
                            objectMapper.readValue(message.getRegistration(), Registration.class);
                    processQueueData(
                            message,
                            sector,
                            registration,
                            eventIssuedEvent.getUserId(),
                            eventIssuedEvent.getEventId());
                    sector.decreaseEventStock();
                } catch (JsonProcessingException e) {
                    log.error("JsonProcessingException: {}", e.getMessage());
                }
            }
        }
    }

    /**
     * 대기열에서 pop한 registration을 저장하고 유저 신청 결과 상태 정보를 메일 전송하는 이벤트를 발행한다.
     *
     * @author : cookie, blackbean
     */
    public void processQueueData(
            ChatMessage chatMessage,
            Sector sector,
            Registration registration,
            Long userId,
            Long eventId) {
        User user = userAdaptor.findById(userId);
        reflectUserState(chatMessage, sector, user, eventId);
        saveRegistration(sector, user, registration);
        Events.raise(
                RegistrationCreationEvent.of(registration, user.getStatus(), user.getSequence()));
    }

    private void reflectUserState(ChatMessage chatMessage, Sector sector, User user, Long eventId) {
        String key = eventId.toString() + "-" + sector.getSectorNumber();
        if (sector.isSectorCapacityRemaining()) user.success();
        else if (sector.isSectorReserveRemaining()) {
            user.prepare(waitingQueueService.getWaitingOrder(key, chatMessage).intValue() + 1);
        } else user.fail();
    }

    private void saveRegistration(Sector sector, User user, Registration registration) {
        if (!registration.isSaved()) {
            registration.updateIsSaved(true);
            registrationAdaptor.saveAndFlush(registration);
            return;
        }
        registration.setSector(sector);
        registration.setUser(user);
        registrationAdaptor.saveAndFlush(registration);
    }

    private boolean isConnectionAvailable() {
        try (Connection connection = dataSource.getConnection()) {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            log.error("Failed to check MySQL connection availability", e);
            return false;
        }
    }
}
