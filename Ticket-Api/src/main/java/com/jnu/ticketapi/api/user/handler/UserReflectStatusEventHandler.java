package com.jnu.ticketapi.api.user.handler;


import com.jnu.ticketdomain.domains.registration.adaptor.RegistrationAdaptor;
import com.jnu.ticketdomain.domains.user.adaptor.UserAdaptor;
import com.jnu.ticketdomain.domains.user.domain.User;
import com.jnu.ticketdomain.domains.user.event.UserReflectStatusEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserReflectStatusEventHandler {

    private static final Logger tracker = LoggerFactory.getLogger("processTracker");

    private final RegistrationAdaptor registrationAdaptor;
    private final UserAdaptor userAdaptor;

    @Async
    @TransactionalEventListener(
            classes = UserReflectStatusEvent.class,
            phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Retryable(
            retryFor = {Exception.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000))
    public void handle(UserReflectStatusEvent event) {
        // 새로운 persistence context에서 User를 조회 (User를 영속화 하기 위해)
        try {
            MDC.put("userId", String.valueOf(event.getUserId()));
            User user = userAdaptor.findById(event.getUserId());
            reflectUserState(event, user);
        } finally {
            MDC.clear();
        }

        // 이메일 발송은 Closed 될때 배치로 돌릴 수 있도록 리펙토링 완료했습니다.
        //        Events.raise(
        //                RegistrationCreationEvent.of(
        //                        event.getRegistration(), user.getStatus().getValue(),
        // user.getSequence()));
    }

    private void reflectUserState(UserReflectStatusEvent event, User user) {
        Integer position =
                registrationAdaptor.findPositionBySavedAt(
                        event.getRegistration().getId(), event.getSector().getId());
        Integer sectorCapacity = event.getSector().getInitSectorCapacity();
        Integer issueAmount = event.getSector().getIssueAmount();
        if (position <= sectorCapacity) {
            user.success();
        } else if (position <= issueAmount) {
            user.prepare(position - sectorCapacity);
        } else {
            user.fail();
        }

        Long savedAt = registrationAdaptor.findById(event.getRegistration().getId()).getSavedAt();
        tracker.info("최종 시간:{}, 최종 순번: {}, 최종 결과: {}", savedAt, position, user.getStatus().getValue());
        userAdaptor.save(user);
    }
}
