package com.jnu.ticketdomain.domains.events.exception;


import com.jnu.ticketcommon.exception.TicketCodeException;

public class InvalidSizeSectorException extends TicketCodeException {
    public static final TicketCodeException EXCEPTION = new InvalidSizeSectorException();

    private InvalidSizeSectorException() {
        super(SectorErrorCode.INVALID_UPDATE_SECTOR_SIZE);
    }
}
