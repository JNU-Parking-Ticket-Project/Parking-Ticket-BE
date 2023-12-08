package com.jnu.ticketapi.api.registration.model.response;


import com.jnu.ticketcommon.message.ResponseMessage;
import com.jnu.ticketdomain.domains.registration.domain.Registration;
import lombok.Builder;

@Builder
public record FinalSaveResponse(String email, String message) {
    public static FinalSaveResponse of(Registration registration) {
        return FinalSaveResponse.builder()
                .email(registration.getEmail())
                .message(ResponseMessage.SUCCESS_FINAL_SAVE)
                .build();
    }
}
