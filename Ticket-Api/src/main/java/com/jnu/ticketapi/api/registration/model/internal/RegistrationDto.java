package com.jnu.ticketapi.api.registration.model.internal;


import com.jnu.ticketdomain.domains.registration.domain.Registration;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import lombok.Builder;

@Builder
public record RegistrationDto(
        Long id,
        Long registrationId,
        String name,
        String email,
        String phoneNum,
        String studentNum,
        boolean isLight,
        String carNum,
        String affiliation,
        String sectorNum) {
    public static List<RegistrationDto> of(List<Registration> registrations) {
        AtomicLong counter = new AtomicLong(1);
        return registrations.stream()
                .map(registration ->
                        RegistrationDto.builder()
                                .id(counter.getAndIncrement()) // id 값을 1부터 순차적으로 부여
                                .registrationId(registration.getId())
                                .name(registration.getName())
                                .email(registration.getEmail())
                                .phoneNum(registration.getPhoneNum())
                                .studentNum(registration.getStudentNum())
                                .isLight(registration.isLight())
                                .carNum(registration.getCarNum())
                                .affiliation(registration.getAffiliation())
                                .sectorNum(registration.getSector().getSectorNumber())
                                .build())
                .collect(Collectors.toList());
    }
}
