package com.jnu.ticketapi.api.auth.model.request;


import com.jnu.ticketcommon.message.ValidationMessage;
import javax.validation.constraints.Email;
import lombok.Builder;

@Builder
public record CheckEmailRequest(
        @Email(message = ValidationMessage.IS_NOT_VALID_EMAIL) String email) {}
