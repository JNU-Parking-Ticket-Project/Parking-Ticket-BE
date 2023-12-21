package com.jnu.ticketapi.api.user.model.request;


import com.jnu.ticketcommon.annotation.Password;
import com.jnu.ticketcommon.message.ValidationMessage;
import lombok.Builder;

public record UpdatePasswordRequest(
        @Password(message = ValidationMessage.IS_NOT_VALID_PASSWORD) String password) {
    @Builder
    public UpdatePasswordRequest {}
}
