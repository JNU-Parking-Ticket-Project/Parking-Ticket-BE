package com.jnu.ticketapi.api.coupon.service;

import static com.jnu.ticketcommon.consts.TicketStatic.REDIS_COUPON_ISSUE_STORE;

import com.jnu.ticketapi.config.SecurityUtils;
import com.jnu.ticketcommon.annotation.UseCase;
import com.jnu.ticketdomain.common.aop.redissonLock.RedissonLock;
import com.jnu.ticketdomain.domains.coupon.adaptor.CouponAdaptor;
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
    public void issueCoupon() {
        // 재고 감소 로직 구현
        Long currentUserId = SecurityUtils.getCurrentUserId();
        couponAdaptor.findOpenCoupon().validateIssuePeriod();
        waitingQueueService.registerQueue(REDIS_COUPON_ISSUE_STORE, currentUserId);
    }

    //    private void processCouponData(String couponData) {
    //        Coupon coupon = new ObjectMapper().convertValue(couponData, Coupon.class);
    //        // TODO 잔여량
    //        List<Sector> sector = coupon.getSector();
    //        sector.forEach(Sector::decreaseCouponStock);
    //        log.info("쿠폰 발급 완료" + coupon.getCouponCode());
    //    }

    @Transactional(readOnly = true)
    public Long getCouponOrder() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        return waitingQueueService.getWaitingOrder("쿠폰 발급 저장소", currentUserId);
    }
}
