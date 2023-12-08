package com.jnu.ticketcommon.dto;

import static com.jnu.ticketcommon.consts.TicketStatic.OK_REQUEST;

import java.time.LocalDateTime;
import lombok.Getter;

@Getter
public class SuccessResponse {
    private final String message;

    public SuccessResponse(String message) {
        this.message = message;
    }
}
