package com.jnu.ticketapi.api.registration.model.response;


import com.jnu.ticketcommon.message.ResponseMessage;
import com.jnu.ticketdomain.domains.registration.domain.Registration;
import lombok.Builder;

@Builder
public record TemporarySaveResponse(String email, String message) {
    public static TemporarySaveResponse of(Registration registration) {
        return TemporarySaveResponse.builder()
                .email(registration.getEmail())
                .message(ResponseMessage.SUCCESS_TEMPORARY_SAVE)
                .build();
    }
}
