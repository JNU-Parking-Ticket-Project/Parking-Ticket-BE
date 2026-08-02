package com.jnu.ticketinfrastructure.model;

public record MailSendResult(boolean successful, String errorMessage) {

    public static MailSendResult success() {
        return new MailSendResult(true, null);
    }

    public static MailSendResult failure(String errorMessage) {
        return new MailSendResult(false, errorMessage);
    }
}
