package com.jnu.ticketdomain.domains.events;

import static org.assertj.core.api.Assertions.assertThat;

import com.jnu.ticketdomain.domains.events.domain.Sector;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SectorTest {

    @Test
    @DisplayName("Redis 잔여 수량이 정원 구간 안이면 정원만 차감해서 동기화한다")
    void syncRemainingAmountDecreasesCapacityFirst() {
        Sector sector =
                Sector.builder()
                        .sectorNumber("1구간")
                        .name("공과대학")
                        .sectorCapacity(3)
                        .reserve(2)
                        .build();

        sector.syncRemainingAmount(4);

        assertThat(sector.getSectorCapacity()).isEqualTo(2);
        assertThat(sector.getReserve()).isEqualTo(2);
        assertThat(sector.getRemainingAmount()).isEqualTo(4);
    }

    @Test
    @DisplayName("Redis 잔여 수량이 예비 구간이면 정원은 0, 예비는 남은 수량으로 동기화한다")
    void syncRemainingAmountDecreasesReserveAfterCapacity() {
        Sector sector =
                Sector.builder()
                        .sectorNumber("1구간")
                        .name("공과대학")
                        .sectorCapacity(3)
                        .reserve(2)
                        .build();

        sector.syncRemainingAmount(1);

        assertThat(sector.getSectorCapacity()).isZero();
        assertThat(sector.getReserve()).isEqualTo(1);
        assertThat(sector.getRemainingAmount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Redis 잔여 수량은 0과 issueAmount 사이로 보정해서 동기화한다")
    void syncRemainingAmountBoundsRemainingAmount() {
        Sector sector =
                Sector.builder()
                        .sectorNumber("1구간")
                        .name("공과대학")
                        .sectorCapacity(3)
                        .reserve(2)
                        .build();

        sector.syncRemainingAmount(-10);

        assertThat(sector.getSectorCapacity()).isZero();
        assertThat(sector.getReserve()).isZero();
        assertThat(sector.getRemainingAmount()).isZero();

        sector.syncRemainingAmount(10);

        assertThat(sector.getSectorCapacity()).isEqualTo(3);
        assertThat(sector.getReserve()).isEqualTo(2);
        assertThat(sector.getRemainingAmount()).isEqualTo(5);
    }
}
