package com.jnu.ticketdomain.out;


import com.jnu.ticketcommon.annotation.Port;
import com.jnu.ticketdomain.domain.user.User;
import java.util.Optional;

@Port
public interface UserLoadPort {
    Optional<User> findByEmail(String email);

    Optional<User> findById(Long id);
}
