package com.example.couponapi.service;


import com.jnu.ticketapi.api.coupon.model.request.WaitingQueueRequestDto;
import com.jnu.ticketinfrastructure.service.CouponIssueService;
import com.jnu.ticketinfrastructure.service.WaitingQueueService;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class WaitingQueueApiService {
    private final WaitingQueueService waitingQueueService;
    private final CouponIssueService couponIssueService;
    private final org.slf4j.Logger log = LoggerFactory.getLogger(this.getClass().getSimpleName());

    public WaitingQueueApiService(
            WaitingQueueService waitingQueueService, CouponIssueService couponIssueService) {
        this.waitingQueueService = waitingQueueService;
        this.couponIssueService = couponIssueService;
    }

    public boolean register(WaitingQueueRequestDto input) {
        if (!canRegister(input.getCouponTitle())) {
            return false;
        }
        Boolean result =
                waitingQueueService.registerQueue(input.getCouponTitle(), input.getUserId());
        return result != null && result;
    }

    public Long getWaitingOrder(String couponTitle, String userId) {
        return waitingQueueService.getWaitingOrder(couponTitle, userId);
    }

    private boolean canRegister(String couponTitle) {
        if (!couponIssueService.issuableCouponDate(couponTitle)) {
            log.info(couponTitle + " 쿠폰 발급 기간이 아닙니다.");
            return false;
        }
        if (!couponIssueService.issuableCouponQuantity(couponTitle)) {
            log.info(couponTitle + " 쿠폰이 모두 발급되었습니다.");
            return false;
        }
        return true;
    }
}
