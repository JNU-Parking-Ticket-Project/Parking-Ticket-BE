package com.jnu.ticketdomain.domains.admin.out;


import com.jnu.ticketdomain.domains.user.domain.User;
import java.util.Optional;

public interface AdminLoadPort {
    Optional<User> findUserById(Long userId);
}
