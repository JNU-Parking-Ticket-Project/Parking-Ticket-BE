package com.jnu.ticketapi.api.registration.model.internal;


import com.jnu.ticketdomain.domains.registration.domain.Registration;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Builder;

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
        String sectorName) {
    public static List<RegistrationDto> of(List<Registration> registrations) {
        return registrations.stream()
                .map(
                        registration ->
                                RegistrationDto.builder()
                                        .registrationId(registration.getId())
                                        .name(registration.getName())
                                        .email(registration.getEmail())
                                        .phoneNum(registration.getPhoneNum())
                                        .studentNum(registration.getStudentNum())
                                        .isLight(registration.isLight())
                                        .carNum(registration.getCarNum())
                                        .affiliation(registration.getAffiliation())
                                        .sectorName(registration.getSector().getName())
                                        .build())
                .collect(Collectors.toList());
    }
}
