package com.jnu.ticketapi.application.port;


import com.jnu.ticketapi.dto.*;
import com.jnu.ticketdomain.domains.registration.domain.Registration;

public interface RegistrationUseCase {
    Registration findByUserId(Long userId);
    GetRegistrationResponseDto getRegistration(Long userId, String email);

}
