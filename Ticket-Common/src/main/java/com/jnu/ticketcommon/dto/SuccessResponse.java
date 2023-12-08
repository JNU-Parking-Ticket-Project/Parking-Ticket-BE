package com.jnu.ticketcommon.dto;

import static com.jnu.ticketcommon.consts.TicketStatic.OK_REQUEST;

import java.time.LocalDateTime;
import lombok.Getter;

@Getter
public class SuccessResponse {

    private final boolean success = true;
    private final int status;
    private final String message;
    private final LocalDateTime timeStamp;

    public SuccessResponse(String message) {
        this.status = OK_REQUEST;
        this.message = message;
        this.timeStamp = LocalDateTime.now();
    }
}
