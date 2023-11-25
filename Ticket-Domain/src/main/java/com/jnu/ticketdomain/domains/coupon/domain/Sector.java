package com.jnu.ticketdomain.domains.coupon.domain;


import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Sector {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sector_id")
    private Long id;

    // 1구간, 2구간 ...
    @Column(name = "sector_number")
    private String sectorNumber;

    // 구간 명 : 단과 대학
    @Column(name = "name", nullable = false)
    private String name;

    // 구간별 정원
    @Column(name = "sector_capacity", nullable = false)
    private Integer sectorCapacity;
    // 예비 정원
    @Column(name = "reserve", nullable = false)
    private Integer reserve;

    // 총 정원
    @Column(name = "total")
    private Integer total;

    @Builder
    public Sector(String sectorNumber, String name, Integer sectorCapacity, Integer reserve) {
        this.sectorNumber = sectorNumber;
        this.name = name;
        this.sectorCapacity = sectorCapacity;
        this.reserve = reserve;
        this.total = sectorCapacity + reserve;
    }

//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "coupon_id", nullable = false)
//    private Coupon coupon;

}
