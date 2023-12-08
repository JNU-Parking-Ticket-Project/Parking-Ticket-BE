package com.jnu.ticketcommon.dto;


import lombok.Getter;

@Getter
public class SuccessResponse {
    private final String message;

    public SuccessResponse(String message) {
        this.message = message;
    }
}
