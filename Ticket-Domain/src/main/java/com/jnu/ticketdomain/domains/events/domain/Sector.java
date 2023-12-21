package com.jnu.ticketdomain.domains.events.domain;


import com.jnu.ticketdomain.domains.events.exception.NoEventStockLeftException;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
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

    @Column(name = "init_sector_capacity", nullable = false)
    private Integer initSectorCapacity;
    // 예비 정원
    @Column(name = "reserve", nullable = false)
    private Integer reserve;

    @Column(name = "init_reserve", nullable = false)
    private Integer initReserve;

    // 총 정원
    @Column(name = "issue_amount", nullable = false)
    private Integer issueAmount;

    @Column(name = "remaining_amount")
    private Integer remainingAmount;

    @ManyToOne
    @JoinColumn(name = "event_id")
    private Event event;

    @Builder
    public Sector(String sectorNumber, String name, Integer sectorCapacity, Integer reserve) {
        this.sectorNumber = sectorNumber;
        this.name = name;
        this.sectorCapacity = sectorCapacity;
        this.initSectorCapacity = sectorCapacity;
        this.reserve = reserve;
        this.initReserve = reserve;
        this.issueAmount = sectorCapacity + reserve;
        this.remainingAmount = this.issueAmount;
    }

    public void resetAmount() {
        this.remainingAmount = this.issueAmount;
        this.sectorCapacity = this.initSectorCapacity;
        this.reserve = this.initReserve;
    }

    public void decreaseEventStock() {
        checkEventLeft();
        if (isSectorRemaining()) {
            decreaseCapacity();
        } else if (isSectorReserveRemaining()) {
            decreaseReserve();
        } else {
            throw NoEventStockLeftException.EXCEPTION;
        }
    }

    private void decreaseCapacity() {
        sectorCapacity--;
        remainingAmount--;
    }

    private void decreaseReserve() {
        reserve--;
        remainingAmount--;
    }

    public void checkEventLeft() {
        if (remainingAmount < 1) { // 재고 없을 경우 에러 처리
            throw NoEventStockLeftException.EXCEPTION;
        }
    }

    public boolean isSectorRemaining() {
        return remainingAmount > 0;
    }

    public boolean isSectorReserveRemaining() {
        return reserve > 0;
    }

    public void update(Sector sector) {
        this.sectorNumber = sector.sectorNumber;
        this.name = sector.name;
        this.sectorCapacity = sector.sectorCapacity;
        this.reserve = sector.reserve;
        this.issueAmount = sector.sectorCapacity + sector.reserve;
        this.remainingAmount = issueAmount;
    }

    public void setEvent(Event savedEvent) {
        this.event = savedEvent;
    }
}
