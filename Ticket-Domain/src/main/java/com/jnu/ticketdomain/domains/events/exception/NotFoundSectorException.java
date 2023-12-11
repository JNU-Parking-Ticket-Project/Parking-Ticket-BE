package com.jnu.ticketdomain.domains.events.exception;


import com.jnu.ticketcommon.exception.TicketCodeException;

public class NotFoundSectorException extends TicketCodeException {
    public static final TicketCodeException EXCEPTION = new NotFoundSectorException();

    private NotFoundSectorException() {
        super(SectorErrorCode.NOT_FOUND_SECTOR);
    }
}
