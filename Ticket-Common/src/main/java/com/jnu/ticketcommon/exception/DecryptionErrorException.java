package com.jnu.ticketcommon.exception;

public class DecryptionErrorException extends TicketCodeException {
    public static final TicketCodeException EXCEPTION = new DecryptionErrorException();

    private DecryptionErrorException() {
        super(GlobalErrorCode.DECRYPTION_ERROR);
    }
}
