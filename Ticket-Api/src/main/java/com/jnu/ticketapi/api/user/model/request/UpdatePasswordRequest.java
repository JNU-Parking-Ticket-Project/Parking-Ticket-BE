package com.jnu.ticketapi.api.user.model.request;


import lombok.Builder;

public record UpdatePasswordRequest(String password) {
    @Builder
    public UpdatePasswordRequest {}
}
