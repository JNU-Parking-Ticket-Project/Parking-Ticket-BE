package com.jnu.ticketapi.api.coupon.handler;

import static com.jnu.ticketcommon.consts.TicketStatic.REDIS_COUPON_ISSUE_STORE;

import com.jnu.ticketdomain.domains.coupon.domain.Sector;
import com.jnu.ticketdomain.domains.registration.adaptor.RegistrationAdaptor;
import com.jnu.ticketinfrastructure.domainEvent.CouponIssuedEvent;
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
public class CouponIssuedEventHandler {
    private final RegistrationAdaptor registrationAdaptor;
    private final WaitingQueueService waitingQueueService;

    @Async
    @TransactionalEventListener(
            classes = CouponIssuedEvent.class,
            phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(CouponIssuedEvent couponIssuedEvent) {
        waitingQueueService.popQueue(REDIS_COUPON_ISSUE_STORE, 1, ChatMessage.class);
        //        messages.forEach(message -> processCouponData(message.getUserId()));
        processCouponData(couponIssuedEvent.getCurrentUserId());
    }

    private void processCouponData(Long userId) {
        Sector sector = registrationAdaptor.findByUserId(userId).getSector();
        sector.checkCouponLeft();
        log.info(sector.getRemainingAmount().toString());
        sector.decreaseCouponStock();
    }
}
