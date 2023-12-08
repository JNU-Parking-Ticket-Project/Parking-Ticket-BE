package com.jnu.ticketdomain.domains.registration.out;


import com.jnu.ticketcommon.annotation.Port;
import com.jnu.ticketdomain.domains.registration.domain.Registration;
import java.util.List;
import java.util.Optional;

@Port
public interface RegistrationLoadPort {
    Registration findByUserId(Long userId);

    Registration findById(Long id);

    List<Registration> findAll();

    Optional<Registration> findByEmail(String email);
}
