package com.jnu.ticketapi.api.registration.model.internal;


import com.jnu.ticketdomain.domains.registration.domain.Registration;
import lombok.Builder;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

@Builder
public record RegistrationDto(
        Long registrationId,
        String name,
        String email,
        String phoneNum,
        String studentNum,
        boolean isLight,
        String carNum,
        String affiliation,
        String department,
        String sectorNum,
        Integer position,
        String resultStatus,
        Integer sequence,
        LocalDateTime savedAt,
        boolean savedAtEstimated) {

    private static final ZoneId SERVICE_ZONE_ID = ZoneId.of("Asia/Seoul");
    private static final long MIN_SUPPORTED_EPOCH_MILLIS = 1_577_836_800_000L;
    private static final long MAX_SUPPORTED_EPOCH_MILLIS = 4_102_444_800_000L;

    public static List<RegistrationDto> of(List<Registration> registrations) {
        return registrations.stream()
                .map(registration -> {
                    SavedAtResolution savedAt = resolveSavedAt(registration);
                    return RegistrationDto.builder()
                            .registrationId(registration.getId())
                            .name(registration.getName())
                            .email(registration.getEmail())
                            .phoneNum(registration.getPhoneNum())
                            .studentNum(registration.getStudentNum())
                            .isLight(registration.isLight())
                            .carNum(registration.getCarNum())
                            .affiliation(registration.getAffiliation())
                            .department(registration.getDepartment())
                            .sectorNum(registration.getSector().getSectorNumber())
                            .position(registration.getPosition())
                            .resultStatus(registration.getResultStatus().getValue())
                            .sequence(registration.getSequence())
                            .savedAt(savedAt.value())
                            .savedAtEstimated(savedAt.estimated())
                            .build();
                })
                .collect(Collectors.toList());

    }

    private static SavedAtResolution resolveSavedAt(Registration registration) {
        Long rawSavedAt = registration.getSavedAt();
        if (rawSavedAt == null) {
            return new SavedAtResolution(registration.getCreatedAt(), true);
        }

        if (isSupportedEpochMillis(rawSavedAt)) {
            return new SavedAtResolution(toLocalDateTime(rawSavedAt), false);
        }

        long microsecondsCandidate = rawSavedAt / 1_000L;
        if (isSupportedEpochMillis(microsecondsCandidate)) {
            // 과거 epoch microseconds 값과 System.nanoTime 값은 크기만으로 완전히 구분할 수 없다.
            return new SavedAtResolution(toLocalDateTime(microsecondsCandidate), true);
        }

        long nanosecondsCandidate = rawSavedAt / 1_000_000L;
        if (isSupportedEpochMillis(nanosecondsCandidate)) {
            return new SavedAtResolution(toLocalDateTime(nanosecondsCandidate), false);
        }

        return new SavedAtResolution(registration.getCreatedAt(), true);
    }

    private static boolean isSupportedEpochMillis(long value) {
        return value >= MIN_SUPPORTED_EPOCH_MILLIS && value <= MAX_SUPPORTED_EPOCH_MILLIS;
    }

    private static LocalDateTime toLocalDateTime(long epochMillis) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), SERVICE_ZONE_ID);
    }

    private record SavedAtResolution(LocalDateTime value, boolean estimated) {}
}
