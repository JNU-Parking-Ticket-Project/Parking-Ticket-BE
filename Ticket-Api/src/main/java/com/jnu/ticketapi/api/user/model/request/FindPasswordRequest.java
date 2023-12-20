package com.jnu.ticketapi.api.user.model.request;


import com.jnu.ticketcommon.message.ValidationMessage;
import com.jnu.ticketdomain.domains.CredentialCode.domain.CredentialCode;
import javax.validation.constraints.Email;
import lombok.Builder;

public record FindPasswordRequest(
        @Email(message = ValidationMessage.IS_NOT_VALID_EMAIL) String email) {
    @Builder
    public FindPasswordRequest {}

    public CredentialCode toEntity(String code) {
        return CredentialCode.builder().code(code).email(this.email).build();
    }
}
