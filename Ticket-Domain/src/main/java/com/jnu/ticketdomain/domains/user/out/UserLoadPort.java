package com.jnu.ticketdomain.domains.user.out;


import com.jnu.ticketcommon.annotation.Port;
import com.jnu.ticketdomain.domains.user.domain.User;
import java.util.Optional;

@Port
public interface UserLoadPort {
    Optional<User> findByEmail(String email);
    Optional<User> findById(Long userId);
}
