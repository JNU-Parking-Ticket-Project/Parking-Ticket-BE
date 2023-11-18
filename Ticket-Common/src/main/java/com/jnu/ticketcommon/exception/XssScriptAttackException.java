package com.jnu.ticketcommon.exception;

public class XssScriptAttackException extends TicketCodeException {

    public static final TicketCodeException EXCEPTION = new XssScriptAttackException();

    private XssScriptAttackException() {
        super(GlobalErrorCode.XSS_SCRIPT_ATTACK);
    }
}
