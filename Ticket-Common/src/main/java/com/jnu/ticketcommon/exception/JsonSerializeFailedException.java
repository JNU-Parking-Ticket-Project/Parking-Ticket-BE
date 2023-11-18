package com.jnu.ticketcommon.exception;

public class JsonSerializeFailedException extends TicketCodeException {

    public static final TicketCodeException EXCEPTION = new JsonSerializeFailedException();

    private JsonSerializeFailedException() {
        super(GlobalErrorCode.DATE_FORMAT_SERIALIZE_ERROR);
    }
}
