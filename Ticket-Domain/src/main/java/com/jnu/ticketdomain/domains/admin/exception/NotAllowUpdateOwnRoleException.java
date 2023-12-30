package com.jnu.ticketdomain.domains.admin.exception;

import com.jnu.ticketcommon.exception.TicketCodeException;

public class NotAllowUpdateOwnRoleException extends TicketCodeException {
    public static final TicketCodeException EXCEPTION = new NotAllowUpdateOwnRoleException();

    private NotAllowUpdateOwnRoleException() {
        super(AdminErrorCode.NOT_ALLOW_UPDATE_OWN_ROLE);
    }
}
