package com.jnu.ticketapi.api.event.handler;

import static com.jnu.ticketcommon.consts.TicketStatic.REDIS_EVENT_ISSUE_STORE;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventIssuedEventHandler {

    private static final Logger tracker = LoggerFactory.getLogger("processTracker");

    private final RegistrationAdaptor registrationAdaptor;
    private final UserAdaptor userAdaptor;

    @Autowired(required = false)
    private WaitingQueueService waitingQueueService;

    private final SectorAdaptor sectorAdaptor;
    private final ObjectMapper objectMapper;
    private final HikariDataSource hikariDataSource;

    private static final String JSON_PARSE_FAIL_COUNT_KEY = "json_parse_fail_count:";
    private static final int MAX_JSON_PARSE_FAIL_COUNT = 3;

    @EventListener(classes = EventIssuedEvent.class)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(EventIssuedEvent eventIssuedEvent) {
        Long userId = eventIssuedEvent.getMessage().getUserId();
        try {
            MDC.put("userId", String.valueOf(userId));
            tracker.info(
                    "이벤트 처리 시작 - 사용자Id: {}, 구간Id: {}",
                    eventIssuedEvent.getMessage().getUserId(),
                    eventIssuedEvent.getMessage().getSectorId());

            // 1. 데이터베이스 연결 풀 확인
            if (!isIdleConnectionAvailable()) {
                tracker.warn("사용 가능한 DB 연결이 없어 현재 처리를 건너뜀");
                return; // 스케줄러가 다시 처리하도록 Redis에서 제거하지 않음
            }

            // 2. JSON 파싱 시도
            Registration registration;
            try {
                registration =
                        objectMapper.readValue(
                                eventIssuedEvent.getMessage().getRegistration(),
                                Registration.class);
            } catch (JsonProcessingException e) {
                String registrationMessage = eventIssuedEvent.getMessage().getRegistration();
                String key = JSON_PARSE_FAIL_COUNT_KEY + userId;
                int failCount = waitingQueueService.incrementFailCount(key);
                if (failCount >= MAX_JSON_PARSE_FAIL_COUNT) {
                    tracker.error(
                            "JSON 파싱 {}회 연속 실패, 큐에서 제거 - UserId: {}, 데이터: {}",
                            failCount,
                            userId,
                            eventIssuedEvent.getMessage().getRegistration());
                    waitingQueueService.clearFailCount(key); // 실패 횟수 초기화
                    waitingQueueService.remove(
                            REDIS_EVENT_ISSUE_STORE, eventIssuedEvent.getMessage()); // 큐에서 제거
                    return;
                } else {
                    tracker.error(
                            "JSON 파싱 실패 ({}/{}회), 재처리를 위해 큐에 유지 - UserId: {}, 데이터: {}",
                            failCount,
                            MAX_JSON_PARSE_FAIL_COUNT,
                            userId,
                            eventIssuedEvent.getMessage().getRegistration(),
                            e);
                    return;
                }
            }

            // 3. 중복 처리 확인
            if (Boolean.TRUE.equals(
                    registrationAdaptor.existsByIdAndIsSavedTrue(registration.getId()))) {
                tracker.info("이미 저장된 등록정보, 큐에서 제거 시작 - RegistrationId: {}", registration.getId());
                registerTransactionSynchronization(eventIssuedEvent, true);
                return;
            }

            // 4. 섹터 정보 조회
            Sector sector = sectorAdaptor.findById(eventIssuedEvent.getMessage().getSectorId());

            tracker.info(
                    "구간 정보 - 구간Id: {}, 정원: {}, 예비: {}, 잔여: {}",
                    sector.getId(),
                    sector.getSectorCapacity(),
                    sector.getReserve(),
                    sector.getRemainingAmount());

            // 5. 등록 처리
            processQueueData(sector, registration, eventIssuedEvent);
            sectorAdaptor.decreaseRemainingAmount(sector.getId());

            // 6. 성공적으로 처리된 경우 트랜잭션 커밋 후 Redis에서 제거
            registerTransactionSynchronization(eventIssuedEvent, true);
            tracker.info("등록 처리 완료 - RegistrationId: {}", registration.getId());

        } catch (NoEventStockLeftException e) {
            // 좌석 부족은 정상적인 비즈니스 로직이므로 Redis에서 제거
            tracker.info(
                    "잔여 좌석 없음, 큐에서 제거 - SectorId: {}", eventIssuedEvent.getMessage().getSectorId());
            registerTransactionSynchronization(eventIssuedEvent, true);

        } catch (Exception e) {
            // 시스템 예외 발생 시 로깅만 하고 Redis에서 제거하지 않음
            // 다음 스케줄러 실행 시 재처리됨
            tracker.error(
                    "시스템 예외 발생, 다음 스케줄러 실행시 재처리를 위해 큐에 유지 - UserId: {}, 오류: {}",
                    eventIssuedEvent.getMessage().getUserId(),
                    e.getMessage(),
                    e);
            // Redis에서 제거하지 않으므로 registerTransactionSynchronization 호출하지 않음

        } finally {
            MDC.clear();
        }
    }

    /**
     * 트랜잭션 커밋/롤백 후 Redis 큐 처리를 위한 콜백 등록
     *
     * @param eventIssuedEvent 처리할 이벤트
     * @param removeOnSuccess 성공 시 제거 여부
     */
    private void registerTransactionSynchronization(
            EventIssuedEvent eventIssuedEvent, boolean removeOnSuccess) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            if (removeOnSuccess) {
                                waitingQueueService.remove(
                                        REDIS_EVENT_ISSUE_STORE, eventIssuedEvent.getMessage());
                                tracker.info(
                                        "트랜잭션 커밋 성공, Redis 큐에서 제거 - UserId: {}",
                                        eventIssuedEvent.getMessage().getUserId());
                            }
                        }

                        @Override
                        public void afterCompletion(int status) {
                            if (status == STATUS_ROLLED_BACK) {
                                tracker.error(
                                        "트랜잭션 롤백됨, 재시도를 위해 Redis 큐에 데이터 유지 - UserId: {}",
                                        eventIssuedEvent.getMessage().getUserId());
                            }
                        }
                    });
        } else {
            tracker.error(
                    "트랜잭션 롤백됨, 재시도를 위해 Redis 큐에 데이터 유지 - UserId: {}",
                    eventIssuedEvent.getMessage().getUserId());
        }
    }

    /** 대기열에서 추출한 등록정보를 저장하고 사용자 신청 결과 상태 정보를 메일 전송하는 이벤트를 발행한다. */
    public void processQueueData(Sector sector, Registration registration, EventIssuedEvent event) {
        Long userId = event.getMessage().getUserId();
        User user = userAdaptor.findById(userId);
        saveRegistration(sector, user, registration, event.getScore());
        Events.raise(UserReflectStatusEvent.of(userId, registration, sector));
    }

    /** 등록 정보를 데이터베이스에 저장 */
    private void saveRegistration(
            Sector sector, User user, Registration registration, Double score) {
        if (!sector.isRemainingAmount()) {
            tracker.info("[잔여 좌석 없음]. RegistrationId: {}", registration.getId());
            throw NoEventStockLeftException.EXCEPTION;
        }

        registration.setSector(sector);
        registration.setUser(user);
        registration.setSavedAt(score.longValue());
        registrationAdaptor.saveAndFlush(registration);
    }

    /**
     * 데이터베이스 연결 풀에 여유 연결이 있는지 확인
     *
     * @return 여유 연결이 2개 이상 있으면 true
     */
    private boolean isIdleConnectionAvailable() {
        try {
            int idleConnections = hikariDataSource.getHikariPoolMXBean().getIdleConnections();

            // 최소 2개 이상의 여유 연결 확보
            boolean available = idleConnections >= 2;

            if (!available) {
                tracker.warn("DB 연결 부족 - 현재: {}", idleConnections);
            }

            return available;
        } catch (Exception e) {
            tracker.error("DB 연결 상태 확인 실패, 처리 진행으로 가정", e);
            return true; // 확인 실패 시 기본적으로 처리 진행
        }
    }
}
