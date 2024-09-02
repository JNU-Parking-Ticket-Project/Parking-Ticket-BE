package com.jnu.ticketapi.api.event.handler;

import static com.jnu.ticketcommon.consts.TicketStatic.REDIS_EVENT_ISSUE_STORE;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jnu.ticketdomain.domains.events.adaptor.SectorAdaptor;
import com.jnu.ticketdomain.domains.events.domain.Sector;
import com.jnu.ticketdomain.domains.events.exception.NoEventStockLeftException;
import com.jnu.ticketdomain.domains.registration.adaptor.RegistrationAdaptor;
import com.jnu.ticketdomain.domains.registration.domain.Registration;
import com.jnu.ticketdomain.domains.user.adaptor.UserAdaptor;
import com.jnu.ticketdomain.domains.user.domain.User;
import com.jnu.ticketinfrastructure.domainEvent.EventIssuedEvent;
import com.jnu.ticketinfrastructure.model.ChatMessage;
import com.jnu.ticketinfrastructure.model.ChatMessageStatus;
import com.jnu.ticketinfrastructure.service.WaitingQueueService;
import com.zaxxer.hikari.HikariDataSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
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
    private final WaitingQueueService waitingQueueService;
    private final SectorAdaptor sectorAdaptor;
    private final ObjectMapper objectMapper;
    private final HikariDataSource hikariDataSource;
    private final StringRedisTemplate stringRedisTemplate;
    private final Map<Sector, AtomicInteger> counter = new ConcurrentHashMap<>();

    @Async
    @EventListener(classes = EventIssuedEvent.class)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(EventIssuedEvent eventIssuedEvent) {
        log.info("주차권 신청 저장 시작");
        if (isIdleConnectionAvailable()) {
            Sector sector = sectorAdaptor.findById(eventIssuedEvent.getMessage().getSectorId());

            try {
                Registration registration =
                        objectMapper.readValue(
                                eventIssuedEvent.getMessage().getRegistration(),
                                Registration.class);
                processQueueData(sector, registration, eventIssuedEvent.getMessage().getUserId());
                waitingQueueService.remove(REDIS_EVENT_ISSUE_STORE, eventIssuedEvent.getMessage());
                sector.decreaseEventStock();
                log.info("주차권 신청 저장 완료");
            } catch (NoEventStockLeftException e) {
                log.error("해당 구간 잔여 여석이 없습니다.");
                waitingQueueService.remove(REDIS_EVENT_ISSUE_STORE, eventIssuedEvent.getMessage());
            } catch (Exception e) {
                // 에러가 났을 때 redis에 데이터를 재등록 한다.(Not Waiting 상태로)
                log.error("EventIssuedEventHandler Exception: ", e);
                ChatMessage message =
                        new ChatMessage(
                                eventIssuedEvent.getMessage().getRegistration(),
                                eventIssuedEvent.getMessage().getUserId(),
                                eventIssuedEvent.getMessage().getSectorId(),
                                eventIssuedEvent.getMessage().getEventId(),
                                ChatMessageStatus.WAITING.name());
                waitingQueueService.reRegisterQueue(
                        REDIS_EVENT_ISSUE_STORE,
                        message,
                        ChatMessageStatus.NOT_WAITING,
                        eventIssuedEvent.getScore());
            }
        }
    }

    /** 대기열에서 pop한 registration을 저장하고 유저 신청 결과 상태 정보를 메일 전송하는 이벤트를 발행한다. */
    public void processQueueData(Sector sector, Registration registration, Long userId) {
        User user = userAdaptor.findById(userId);
        reflectUserState(sector, user);
        saveRegistration(sector, user, registration);
        //        Events.raise(
        //                RegistrationCreationEvent.of(registration, user.getStatus(),
        // user.getSequence()));
    } // 이진혁 바보 멍청이 말미잘

    private void reflectUserState(Sector sector, User user) {
        AtomicInteger sectorCounter = counter.computeIfAbsent(sector, k -> new AtomicInteger(1));
        if (sector.isSectorCapacityRemaining()) {
            user.success();
        } else if (sector.isSectorReserveRemaining()) {
            user.prepare(sectorCounter.get());
            increment(sector);
        } else {
            user.fail();
        }
    }

    private void saveRegistration(Sector sector, User user, Registration registration) {
        if (!sector.isRemainingAmount()) {
            throw NoEventStockLeftException.EXCEPTION;
        }
        if (!registration.isSaved()) {
            registration.updateIsSaved(true);
            registration.setSector(sector);
            registration.setUser(user);
            registrationAdaptor.save(registration);
            return;
        }
        registration.setSector(sector);
        registration.setUser(user);
        registrationAdaptor.saveAndFlush(registration);
    }

    private boolean isIdleConnectionAvailable() {
        int idleConnections = hikariDataSource.getHikariPoolMXBean().getIdleConnections();
        return idleConnections > 0;
    }

    public void increment(Sector sector) {
        AtomicInteger sectorCounter = counter.get(sector);
        while (true) {
            int existingValue = sectorCounter.get();
            int newValue = existingValue + 1;
            if (sectorCounter.compareAndSet(existingValue, newValue)) {
                return;
            }
        }
    }
}
