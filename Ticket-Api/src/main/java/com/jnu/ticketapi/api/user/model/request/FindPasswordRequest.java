package com.jnu.ticketapi.api.user.model.request;


import com.jnu.ticketdomain.domains.CredentialCode.domain.CredentialCode;
import lombok.Builder;

public record FindPasswordRequest(String email) {
    @Builder
    public FindPasswordRequest {}

    public CredentialCode toEntity(String code) {
        return CredentialCode.builder().code(code).email(this.email).build();
    }
}
