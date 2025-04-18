package com.jnu.ticketapi.api.event.handler;

import static com.jnu.ticketcommon.consts.TicketStatic.REDIS_EVENT_ISSUE_STORE;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jnu.ticketdomain.common.domainEvent.Events;
import com.jnu.ticketdomain.domains.events.adaptor.SectorAdaptor;
import com.jnu.ticketdomain.domains.events.domain.Sector;
import com.jnu.ticketdomain.domains.events.exception.NoEventStockLeftException;
import com.jnu.ticketdomain.domains.registration.adaptor.RegistrationAdaptor;
import com.jnu.ticketdomain.domains.registration.domain.Registration;
import com.jnu.ticketdomain.domains.user.adaptor.UserAdaptor;
import com.jnu.ticketdomain.domains.user.domain.User;
import com.jnu.ticketdomain.domains.user.event.UserReflectStatusEvent;
import com.jnu.ticketinfrastructure.domainEvent.EventIssuedEvent;
import com.jnu.ticketinfrastructure.service.WaitingQueueService;
import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventIssuedEventHandler {
    private final RegistrationAdaptor registrationAdaptor;
    private final UserAdaptor userAdaptor;

    @Autowired(required = false)
    private WaitingQueueService waitingQueueService;

    private final SectorAdaptor sectorAdaptor;
    private final ObjectMapper objectMapper;
    private final HikariDataSource hikariDataSource;

    @Async
    @EventListener(classes = EventIssuedEvent.class)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(EventIssuedEvent eventIssuedEvent) {
        if (isIdleConnectionAvailable()) {
            Sector sector = sectorAdaptor.findById(eventIssuedEvent.getMessage().getSectorId());

            try {
                Registration registration =
                        objectMapper.readValue(
                                eventIssuedEvent.getMessage().getRegistration(),
                                Registration.class);

                if (Boolean.TRUE.equals(
                        registrationAdaptor.existsByIdAndIsSavedTrue(registration.getId()))) return;

                processQueueData(sector, registration, eventIssuedEvent.getMessage().getUserId());
                waitingQueueService.remove(REDIS_EVENT_ISSUE_STORE, eventIssuedEvent.getMessage());
                sector.decreaseEventStock();
            } catch (NoEventStockLeftException e) {
                log.info("해당 구간 잔여 여석이 없습니다.");
                waitingQueueService.remove(REDIS_EVENT_ISSUE_STORE, eventIssuedEvent.getMessage());
            } catch (Exception e) {
                // 에러가 났을 때 redis에 데이터를 재등록 한다.(Not Waiting 상태로)
                log.error("EventIssuedEventHandler Exception: ", e);
            }
        }
    }

    /** 대기열에서 pop한 registration을 저장하고 유저 신청 결과 상태 정보를 메일 전송하는 이벤트를 발행한다. */
    public void processQueueData(Sector sector, Registration registration, Long userId) {
        User user = userAdaptor.findById(userId);
        saveRegistration(sector, user, registration);
        Events.raise(UserReflectStatusEvent.of(userId, registration, sector));
    }

    private void saveRegistration(Sector sector, User user, Registration registration) {
        if (!sector.isRemainingAmount()) {
            log.info("[No seats remaining]. Registration: {}", registration);
            throw NoEventStockLeftException.EXCEPTION;
        }

        if (!registration.isSaved()) {
            //if문 사용 안됨.
            registration.finalSave();
            registration.setSector(sector);
            registration.setUser(user);
            registrationAdaptor.save(registration);
            return;
        }
        registration.setSector(sector);
        registration.setUser(user);
        registrationAdaptor.saveAndFlush(registration);
        registrationAdaptor.updateSavedAt(registration);
    }

    private boolean isIdleConnectionAvailable() {
        int idleConnections = hikariDataSource.getHikariPoolMXBean().getIdleConnections();
        return idleConnections > 0;
    }
}
