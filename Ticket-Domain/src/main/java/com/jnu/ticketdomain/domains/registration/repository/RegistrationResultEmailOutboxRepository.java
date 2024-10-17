package com.jnu.ticketdomain.domains.registration.repository;

import com.jnu.ticketdomain.domains.registration.domain.RegistrationResultEmailOutbox;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegistrationResultEmailOutboxRepository extends JpaRepository<RegistrationResultEmailOutbox, String> {
}
