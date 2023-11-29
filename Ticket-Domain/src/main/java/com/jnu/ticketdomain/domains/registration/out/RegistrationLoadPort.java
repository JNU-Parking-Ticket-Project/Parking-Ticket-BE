package com.jnu.ticketdomain.domains.registration.out;


import com.jnu.ticketcommon.annotation.Port;
import com.jnu.ticketdomain.domains.registration.domain.Registration;

@Port
public interface RegistrationLoadPort {
    Registration findByUserId(Long userId);

    Registration findById(Long id);
}
