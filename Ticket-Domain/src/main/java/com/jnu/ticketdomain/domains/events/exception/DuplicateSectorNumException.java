package com.jnu.ticketdomain.domains.events.exception;


import com.jnu.ticketcommon.exception.TicketCodeException;

public class DuplicateSectorNumException extends TicketCodeException {
    public static final TicketCodeException EXCEPTION = new DuplicateSectorNumException();

    private DuplicateSectorNumException() {
        super(SectorErrorCode.DUPLICATE_SECTOR_NUMBER);
    }
}
