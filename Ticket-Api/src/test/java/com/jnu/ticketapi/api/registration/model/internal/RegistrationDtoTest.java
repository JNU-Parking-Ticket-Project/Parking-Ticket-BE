package com.jnu.ticketapi.api.registration.model.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.jnu.ticketdomain.domains.events.domain.Sector;
import com.jnu.ticketdomain.domains.registration.domain.Registration;
import com.jnu.ticketdomain.domains.user.domain.UserStatus;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RegistrationDtoTest {

    private static final ZoneId SERVICE_ZONE_ID = ZoneId.of("Asia/Seoul");
    private static final LocalDateTime SAVED_AT = LocalDateTime.of(2026, 7, 29, 12, 0);
    private static final long EPOCH_MILLIS =
            SAVED_AT.atZone(SERVICE_ZONE_ID).toInstant().toEpochMilli();

    @Test
    @DisplayName("현재 Stream의 epoch milliseconds 신청 시간을 그대로 변환한다")
    void convertsEpochMilliseconds() {
        RegistrationDto dto = dto(EPOCH_MILLIS, SAVED_AT.minusMinutes(1));

        assertThat(dto.savedAt()).isEqualTo(SAVED_AT);
        assertThat(dto.savedAtEstimated()).isFalse();
    }

    @Test
    @DisplayName("과거 DB의 epoch microseconds 신청 시간을 변환하고 추정값임을 표시한다")
    void convertsEpochMicrosecondsAsEstimated() {
        RegistrationDto dto = dto(EPOCH_MILLIS * 1_000L, SAVED_AT.minusMinutes(1));

        assertThat(dto.savedAt()).isEqualTo(SAVED_AT);
        assertThat(dto.savedAtEstimated()).isTrue();
    }

    @Test
    @DisplayName("초기 epoch nanoseconds 신청 시간을 변환한다")
    void convertsEpochNanoseconds() {
        RegistrationDto dto = dto(EPOCH_MILLIS * 1_000_000L, SAVED_AT.minusMinutes(1));

        assertThat(dto.savedAt()).isEqualTo(SAVED_AT);
        assertThat(dto.savedAtEstimated()).isFalse();
    }

    @Test
    @DisplayName("절대 시각으로 복원할 수 없는 값은 생성 시각으로 대체하고 추정값임을 표시한다")
    void fallsBackToCreatedAtForUnresolvableMonotonicTime() {
        LocalDateTime createdAt = LocalDateTime.of(2024, 12, 1, 10, 0);

        RegistrationDto dto = dto(86_400_000_000_000L, createdAt);

        assertThat(dto.savedAt()).isEqualTo(createdAt);
        assertThat(dto.savedAtEstimated()).isTrue();
    }

    private RegistrationDto dto(Long savedAt, LocalDateTime createdAt) {
        Sector sector =
                Sector.builder()
                        .sectorNumber("1구간")
                        .name("공과대학")
                        .sectorCapacity(1)
                        .reserve(1)
                        .build();
        Registration registration =
                Registration.builder()
                        .email("student@jnu.ac.kr")
                        .name("학생")
                        .studentNum("20260001")
                        .carNum("12가3456")
                        .isLight(false)
                        .phoneNum("010-0000-0000")
                        .createdAt(createdAt)
                        .savedAt(savedAt)
                        .sector(sector)
                        .isSaved(true)
                        .position(1)
                        .resultStatus(UserStatus.SUCCESS)
                        .sequence(-2)
                        .build();
        registration.setId(1L);

        return RegistrationDto.of(List.of(registration)).get(0);
    }
}
