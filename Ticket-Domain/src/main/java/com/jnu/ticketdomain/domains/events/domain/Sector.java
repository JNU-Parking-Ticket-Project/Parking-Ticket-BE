package com.jnu.ticketdomain.domains.events.domain;


import com.jnu.ticketdomain.domains.events.exception.NoEventStockLeftException;
import com.jnu.ticketdomain.domains.registration.domain.Registration;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Where;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Getter
@Where(clause = "is_deleted = false")
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

    @Column(name = "is_deleted")
    private boolean isDeleted;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    private Event event;

    @OneToMany(mappedBy = "sector", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Registration> registrations = new ArrayList<>();

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
        this.isDeleted = false;
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
        this.initReserve = sector.reserve;
        this.initSectorCapacity = sector.sectorCapacity;
        this.remainingAmount = issueAmount;
        this.isDeleted = sector.isDeleted;
    }

    public void setEvent(Event savedEvent) {
        this.event = savedEvent;
    }
}
