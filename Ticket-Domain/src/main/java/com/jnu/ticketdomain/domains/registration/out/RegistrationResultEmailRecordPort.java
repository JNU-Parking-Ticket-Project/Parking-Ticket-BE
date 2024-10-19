package com.jnu.ticketdomain.domains.registration.out;

public interface RegistrationResultEmailRecordPort {
    void updateEmailTransferResult(String id, boolean emailTransferResult);
}
