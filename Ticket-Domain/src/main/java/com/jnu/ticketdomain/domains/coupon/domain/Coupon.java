package com.jnu.ticketdomain.domains.coupon.domain;


import com.jnu.ticketdomain.common.vo.DateTimePeriod;
import com.jnu.ticketdomain.domains.coupon.exception.NotIssuingCouponPeriodException;
import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class Coupon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "coupon_id")
    private Long id;

    // 쿠폰 pubsub을 위한 일련번호
    @Column(name = "coupon_code")
    private String couponCode;

    // 쿠폰 발행 가능 기간
    @Embedded private DateTimePeriod dateTimePeriod;

    @Embedded private CouponStockInfo couponStockInfo;

    public void decreaseCouponStock() {
        couponStockInfo.decreaseCouponStock();
    }

    public void validateIssuePeriod() {
        LocalDateTime nowTime = LocalDateTime.now();
        if (!dateTimePeriod.contains(nowTime)) {
            throw NotIssuingCouponPeriodException.EXCEPTION;
        }
    }
}
