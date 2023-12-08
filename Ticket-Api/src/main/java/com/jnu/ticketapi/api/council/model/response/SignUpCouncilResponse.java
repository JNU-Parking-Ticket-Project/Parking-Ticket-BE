package com.jnu.ticketapi.api.council.model.response;


import lombok.Builder;

@Builder
public record SignUpCouncilResponse(String message) {
    public static SignUpCouncilResponse of(String message) {
        return SignUpCouncilResponse.builder().message(message).build();
    }
}
