package com.jnu.ticketcommon.exception;


import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TicketDynamicException extends RuntimeException {
    private final int status;
    private final String code;
    private final String reason;
}
