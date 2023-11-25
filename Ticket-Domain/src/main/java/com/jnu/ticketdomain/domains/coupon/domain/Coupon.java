package com.jnu.ticketdomain.domains.coupon.domain;


import com.jnu.ticketdomain.common.vo.DateTimePeriod;
import com.jnu.ticketdomain.domains.coupon.exception.NotIssuingCouponPeriodException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
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

    // 쿠폰 발행 가능 기간
    @Embedded private DateTimePeriod dateTimePeriod;

    @Embedded private CouponStockInfo couponStockInfo;

    // 구간별 정보
    @OneToMany
    @JoinColumn(name = "sector_id")
    private List<Sector> sector;

    @Builder
    public Coupon(DateTimePeriod dateTimePeriod, List<Sector> sector) {
        this.couponCode = UUID.randomUUID().toString().substring(0, 6);
        this.dateTimePeriod = dateTimePeriod;
        this.couponStockInfo = getCouponStockInfo(sector);
        this.sector = sector;
    }

    public void decreaseCouponStock() {
        couponStockInfo.decreaseCouponStock();
    }

    public void validateIssuePeriod() {
        LocalDateTime nowTime = LocalDateTime.now();
        if (!dateTimePeriod.contains(nowTime)) {
            throw NotIssuingCouponPeriodException.EXCEPTION;
        }
    }
    public CouponStockInfo getCouponStockInfo(List<Sector> sectors) {
        Integer total = sectors.stream()
                .map(Sector::getTotal)
                .reduce(0, Integer::sum);
        return CouponStockInfo.builder()
                .issuedAmount(total)
                .remainingAmount(total)
                .build();
    }
}
