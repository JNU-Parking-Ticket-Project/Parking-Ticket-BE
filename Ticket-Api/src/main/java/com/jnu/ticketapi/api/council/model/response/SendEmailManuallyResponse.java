package com.jnu.ticketapi.api.council.model.response;


import lombok.Builder;

@Builder
public record SendEmailManuallyResponse(String message) {
    public static SendEmailManuallyResponse of(String message) {
        return SendEmailManuallyResponse.builder().message(message).build();
    }
}
