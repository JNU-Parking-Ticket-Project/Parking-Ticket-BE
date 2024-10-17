package com.jnu.ticketdomain.domains.registration.out;

import com.jnu.ticketdomain.domains.registration.domain.TransferStatus;

public interface RegistrationResultEmailOutboxRecordPort {
    void updateRegistrationResultEmailTransferResult(String id, boolean emailTransferResult);
}
