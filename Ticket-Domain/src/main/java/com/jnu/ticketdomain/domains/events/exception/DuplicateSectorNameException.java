package com.jnu.ticketdomain.domains.events.exception;


import com.jnu.ticketcommon.exception.TicketCodeException;

public class DuplicateSectorNameException extends TicketCodeException {
    public static final TicketCodeException EXCEPTION = new DuplicateSectorNameException();

    private DuplicateSectorNameException() {
        super(SectorErrorCode.DUPLICATE_SECTOR_NUMBER);
    }
}
