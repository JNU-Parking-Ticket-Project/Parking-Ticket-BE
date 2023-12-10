package com.jnu.ticketapi.api.coupon.service;

import static com.jnu.ticketcommon.consts.TicketStatic.REDIS_COUPON_ISSUE_STORE;

import com.jnu.ticketapi.config.SecurityUtils;
import com.jnu.ticketcommon.annotation.UseCase;
import com.jnu.ticketdomain.common.aop.redissonLock.RedissonLock;
import com.jnu.ticketdomain.domains.coupon.adaptor.CouponAdaptor;
import com.jnu.ticketdomain.domains.coupon.domain.Coupon;
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
    private final CouponAdaptor couponAdaptor;
    /** 재고 감소 */
    @Transactional
    @RedissonLock(
            LockName = "주차권_발급",
            waitTime = 3000,
            leaseTime = 3000,
            timeUnit = TimeUnit.MILLISECONDS)
    public void issueCoupon(Long userId) {
        // 재고 감소 로직 구현
        Coupon openCoupon = couponAdaptor.findOpenCoupon();
        openCoupon.validateIssuePeriod();
        waitingQueueService.registerQueue(REDIS_COUPON_ISSUE_STORE, userId);
    }

    @Transactional(readOnly = true)
    public Long getCouponOrder() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        return waitingQueueService.getWaitingOrder(REDIS_COUPON_ISSUE_STORE, currentUserId);
    }
}
