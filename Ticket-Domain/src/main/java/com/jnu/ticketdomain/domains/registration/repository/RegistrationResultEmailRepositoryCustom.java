package com.jnu.ticketdomain.domains.registration.repository;


import com.jnu.ticketdomain.domains.registration.domain.RegistrationResultEmail;
import java.util.List;

public interface RegistrationResultEmailRepositoryCustom {

    List<RegistrationResultEmail> findOutboxEmailsByEventId(long eventId, int fetchSize);
}
