package com.jnu.ticketapi.api.registration.model.response;

import com.jnu.ticketapi.api.registration.model.internal.RegistrationDto;
import com.jnu.ticketdomain.domains.registration.domain.Registration;
import lombok.Builder;

import java.util.List;
@Builder
public record GetRegistrationsResponse(List<RegistrationDto> registrations) {
    public static GetRegistrationsResponse of(List<Registration> registrations) {
        return GetRegistrationsResponse.builder().registrations(RegistrationDto.of(registrations)).build();
    }
}
