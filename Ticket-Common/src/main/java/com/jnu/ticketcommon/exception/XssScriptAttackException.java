package com.jnu.ticketcommon.exception;

public class XssScriptAttackException extends RecruitCodeException {

    public static final RecruitCodeException EXCEPTION = new XssScriptAttackException();

    private XssScriptAttackException() {
        super(GlobalErrorCode.XSS_SCRIPT_ATTACK);
    }
}
