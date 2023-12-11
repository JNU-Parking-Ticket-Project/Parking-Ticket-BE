package com.jnu.ticketapi.api.coupon.service;

import static com.jnu.ticketcommon.consts.TicketStatic.REDIS_COUPON_ISSUE_STORE;

import com.jnu.ticketapi.config.SecurityUtils;
import com.jnu.ticketcommon.annotation.UseCase;
import com.jnu.ticketdomain.common.aop.redissonLock.RedissonLock;
import com.jnu.ticketdomain.common.vo.DateTimePeriod;
import com.jnu.ticketdomain.domains.events.adaptor.EventAdaptor;
import com.jnu.ticketdomain.domains.events.domain.Event;
import com.jnu.ticketinfrastructure.service.WaitingQueueService;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class CouponWithDrawUseCase {

    private final WaitingQueueService waitingQueueService;
    private final EventAdaptor eventAdaptor;

    /** 재고 감소 */
    @Transactional
    @RedissonLock(
            LockName = "주차권_발급",
            waitTime = 3000,
            leaseTime = 3000,
            timeUnit = TimeUnit.MILLISECONDS)
    public void issueCoupon(Long userId) {
        // 재고 감소 로직 구현
        Event openEvent = eventAdaptor.findOpenCoupon();
        openEvent.validateIssuePeriod();
        waitingQueueService.registerQueue(REDIS_COUPON_ISSUE_STORE, userId);
    }

    @Transactional(readOnly = true)
    public Long getCouponOrder() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        return waitingQueueService.getWaitingOrder(REDIS_COUPON_ISSUE_STORE, currentUserId);
    }

    public DateTimePeriod getCouponPeriod() {
        Event openEvent = eventAdaptor.findOpenCoupon();
        return openEvent.getDateTimePeriod();
    }
}
