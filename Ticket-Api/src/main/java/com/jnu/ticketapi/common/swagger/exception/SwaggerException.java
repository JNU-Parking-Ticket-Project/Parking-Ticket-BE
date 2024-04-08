package com.jnu.ticketapi.common.swagger.exception;


import com.jnu.ticketcommon.exception.TicketCodeException;

public class SwaggerException extends TicketCodeException {
    public static final TicketCodeException EXCEPTION = new SwaggerException();

    private SwaggerException() {
        super(SwaggerErrorCode.PROFILE_IS_PROD);
    }
}
