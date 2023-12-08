package com.jnu.ticketapi.api.auth.model.response;


import lombok.Builder;

@Builder
public record CheckEmailResponse(String message) {
    public static CheckEmailResponse of(String message) {
        return CheckEmailResponse.builder().message(message).build();
    }
}
