package com.jnu.ticketdomain.domains.registration.repository;


import com.jnu.ticketdomain.domains.registration.domain.RegistrationResultEmail;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegistrationResultEmailRepository
        extends JpaRepository<RegistrationResultEmail, String>,
                RegistrationResultEmailRepositoryCustom {}
