package com.jnu.ticketdomain.domains.registration.adaptor;

import com.jnu.ticketcommon.annotation.Adaptor;
import com.jnu.ticketdomain.domains.registration.domain.RegistrationResultEmailDto;
import com.jnu.ticketdomain.domains.registration.out.RegistrationResultEmailOutboxLoadPort;
import com.jnu.ticketdomain.domains.registration.out.RegistrationResultEmailOutboxRecordPort;
import com.jnu.ticketdomain.domains.registration.repository.RegistrationResultEmailOutboxRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
@Adaptor
public class RegistrationResultEmailOutboxAdaptor implements RegistrationResultEmailOutboxLoadPort, RegistrationResultEmailOutboxRecordPort {
    private final RegistrationResultEmailOutboxRepository registrationResultEmailOutboxRepository;

    @Override
    public List<RegistrationResultEmailDto> findWaitingRegistrationResultEmailsByEventIdWithThreshold(Long eventId) {
        return null;
//        return registrationResultEmailOutboxRepository.findWaitingRegistrationResultEmailsByEventIdWithThreshold(eventId, REGISTRATION_SIZE);
    }

    @Override
    public void updateRegistrationResultEmailTransferResult(String id, boolean emailTransferResult) {
//        registrationResultEmailOutboxRepository.updateRegistrationResultEmailTransferResult(emailTransferResult);
    }
}
