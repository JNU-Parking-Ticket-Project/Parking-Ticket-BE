package com.jnu.ticketdomain.domains.events.exception;


import com.jnu.ticketcommon.exception.TicketCodeException;

public class InvalidSectorCapacityAndRemainException extends TicketCodeException {
    public static final TicketCodeException EXCEPTION =
            new InvalidSectorCapacityAndRemainException();

    private InvalidSectorCapacityAndRemainException() {
        super(SectorErrorCode.INVALID_SECTOR_CAPACITY_AND_REMAIN);
    }
}
