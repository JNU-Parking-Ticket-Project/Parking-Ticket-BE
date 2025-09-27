package com.jnu.ticketcommon.exception;

public class JsonProcessErrorException extends TicketCodeException {
    public static final TicketCodeException EXCEPTION = new JsonProcessErrorException();

    private JsonProcessErrorException() {
        super(GlobalErrorCode.JSON_PROCESSING_ERROR);
    }
}
