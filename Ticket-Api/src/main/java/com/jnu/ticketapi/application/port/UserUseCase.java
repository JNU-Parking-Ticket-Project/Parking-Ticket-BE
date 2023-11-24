package com.jnu.ticketapi.application.port;


import com.jnu.ticketdomain.domain.user.User;
import java.util.Optional;

public interface UserUseCase {
    Optional<User> findByEmail(String email);

    User save(User user);
}
