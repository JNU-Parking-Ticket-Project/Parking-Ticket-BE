package com.jnu.ticketdomain.domains.coupon.domain;


import com.jnu.ticketdomain.common.domainEvent.Events;
import com.jnu.ticketdomain.common.vo.DateTimePeriod;
import com.jnu.ticketdomain.domains.coupon.event.CouponExpiredEvent;
import com.jnu.ticketdomain.domains.coupon.exception.InvalidPeriodCouponException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import javax.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class Coupon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "coupon_id")
    private Long id;

    // 쿠폰 pubsub을 위한 일련번호 -> UUID String 6자리
    @Column(name = "coupon_code")
    private String couponCode;

    @Embedded private DateTimePeriod dateTimePeriod;

    @Enumerated(EnumType.STRING)
    private CouponStatus couponStatus;
    // 쿠폰 발행 가능 기간

    // 구간별 정보
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "sector_id")
    private List<Sector> sector;

    @Builder
    public Coupon(DateTimePeriod dateTimePeriod, List<Sector> sector) {
        this.couponCode = UUID.randomUUID().toString().substring(0, 6);
        this.dateTimePeriod = dateTimePeriod;
        this.sector = sector;
        this.couponStatus = CouponStatus.READY;
    }

    @PostPersist
    public void postPersist() {
        Events.raise(CouponExpiredEvent.from(dateTimePeriod));
    }

    public void validateIssuePeriod() {
        LocalDateTime nowTime = LocalDateTime.now();
        if (dateTimePeriod.contains(nowTime)
                || dateTimePeriod.getEndAt().isBefore(nowTime)
                || dateTimePeriod.getEndAt().isBefore(dateTimePeriod.getStartAt())) {
            throw InvalidPeriodCouponException.EXCEPTION;
        }
    }
}
