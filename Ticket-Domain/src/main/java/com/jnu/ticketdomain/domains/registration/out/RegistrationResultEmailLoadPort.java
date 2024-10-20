package com.jnu.ticketdomain.domains.registration.out;


import com.jnu.ticketdomain.domains.registration.domain.RegistrationResultEmail;
import com.jnu.ticketdomain.domains.registration.domain.RegistrationResultEmailDto;
import java.util.List;

public interface RegistrationResultEmailLoadPort {

    RegistrationResultEmail getById(String id);

    List<RegistrationResultEmailDto> findOutboxEmailsByEventIdWithThreshold(Long eventId);
}
