package com.jnu.ticketcommon.exception;

public class EncryptionErrorException extends TicketCodeException {
    public static final TicketCodeException EXCEPTION = new EncryptionErrorException();

    private EncryptionErrorException() {
        super(GlobalErrorCode.ENCRYPTION_ERROR);
    }
}
