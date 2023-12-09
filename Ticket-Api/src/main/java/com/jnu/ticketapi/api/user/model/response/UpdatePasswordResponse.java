package com.jnu.ticketapi.api.user.model.response;


import com.jnu.ticketdomain.domains.user.domain.User;
import lombok.Builder;

public record UpdatePasswordResponse(String email) {
    @Builder
    public UpdatePasswordResponse {}

    public static UpdatePasswordResponse from(User user) {
        return UpdatePasswordResponse.builder().email(user.getEmail()).build();
    }
}
