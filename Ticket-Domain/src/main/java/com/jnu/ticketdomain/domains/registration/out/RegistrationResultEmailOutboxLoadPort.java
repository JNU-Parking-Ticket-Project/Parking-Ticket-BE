package com.jnu.ticketdomain.domains.registration.out;

import com.jnu.ticketdomain.domains.registration.domain.RegistrationResultEmailDto;

import java.util.List;

public interface RegistrationResultEmailOutboxLoadPort {
    List<RegistrationResultEmailDto> findWaitingRegistrationResultEmailsByEventIdWithThreshold(Long eventId);
}
