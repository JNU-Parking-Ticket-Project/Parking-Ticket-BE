package com.jnu.ticketapi.api.user.handler;


import com.jnu.ticketdomain.domains.registration.adaptor.RegistrationAdaptor;
import com.jnu.ticketdomain.domains.registration.adaptor.RegistrationResultEmailAdaptor;
import com.jnu.ticketdomain.domains.user.adaptor.UserAdaptor;
import com.jnu.ticketdomain.domains.user.domain.User;
import com.jnu.ticketdomain.domains.user.event.UserReflectStatusEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final RegistrationAdaptor registrationAdaptor;
    private final RegistrationResultEmailAdaptor registrationResultEmailAdaptor;
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
        User user = userAdaptor.findById(event.getUserId());
        reflectUserState(event, user);

        // (2024.09.04, blackbean99) 이메일 발송은 Closed 될때 배치로 돌릴 수 있도록 리펙토링 완료했습니다.
        // (2024.10.19, sckwon770) 이메일 발송은 Transactional Email Outbox를 도입해 신뢰성 있는 준실시간으로 발송되도록 리팩토링
        // 완료했습니다.
        registrationResultEmailAdaptor.save(
                event.getEventId(),
                user.getEmail(),
                event.getRegistration().getName(),
                user.getStatus().getValue(),
                user.getSequence());
    }

    private void reflectUserState(UserReflectStatusEvent event, User user) {
        Integer position =
                registrationAdaptor.findPositionById(
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
        userAdaptor.save(user);
    }
}
