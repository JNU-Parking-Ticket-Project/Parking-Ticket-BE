package com.jnu.ticketdomain.domains.admin.exception;


import com.jnu.ticketcommon.exception.TicketCodeException;

public class AlreadyExistAdminException extends TicketCodeException {
    public static final TicketCodeException EXCEPTION = new AlreadyExistAdminException();

    private AlreadyExistAdminException() {
        super(AdminErrorCode.ALREADY_EXIST_ADMIN);
    }
}
