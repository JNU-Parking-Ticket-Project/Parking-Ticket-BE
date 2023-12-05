package com.jnu.ticketapi.application.port;


import com.jnu.ticketapi.api.registration.model.request.FinalSaveRequest;
import com.jnu.ticketapi.api.registration.model.request.TemporarySaveRequest;
import com.jnu.ticketapi.api.registration.model.response.FinalSaveResponse;
import com.jnu.ticketapi.api.registration.model.response.GetRegistrationResponse;
import com.jnu.ticketapi.api.registration.model.response.TemporarySaveResponse;
import com.jnu.ticketdomain.domains.registration.domain.Registration;

public interface RegistrationUseCase {
    Registration findByUserId(Long userId);

    GetRegistrationResponse getRegistration(String email);

    TemporarySaveResponse temporarySave(TemporarySaveRequest requestDto, String email);

    Registration save(Registration registration);

    FinalSaveResponse finalSave(FinalSaveRequest requestDto, String email);
}
