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
        LocalDateTime savedAt) {
    public static List<RegistrationDto> of(List<Registration> registrations) {
        return registrations.stream()
                .map(registration -> {
                    Long savedAt = registration.getSavedAt();
                    LocalDateTime localDateTime = LocalDateTime.ofInstant(
                            Instant.ofEpochMilli(savedAt),
                            ZoneId.systemDefault()
                    );
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
                            .savedAt(localDateTime)
                            .build();
                })
                .collect(Collectors.toList());

    }
}
