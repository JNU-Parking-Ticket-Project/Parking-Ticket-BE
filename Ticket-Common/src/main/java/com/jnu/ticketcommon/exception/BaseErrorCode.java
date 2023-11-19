package com.jnu.ticketcommon.exception;

public interface BaseErrorCode {
    public ErrorReason getErrorReason();

    String getExplainError() throws NoSuchFieldException;
}
