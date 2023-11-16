package com.jnu.ticketdomain.domains.out;


import com.jnu.ticketdomain.domains.user.domian.User;
import java.util.Optional;

public interface UserLoadPort {
    Optional<User> findByEmail(String email);
}
