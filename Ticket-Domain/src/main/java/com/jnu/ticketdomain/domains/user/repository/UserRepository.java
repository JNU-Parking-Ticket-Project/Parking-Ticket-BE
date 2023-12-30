package com.jnu.ticketdomain.domains.user.repository;


import com.jnu.ticketdomain.domains.user.domain.User;
import java.util.Optional;

import com.jnu.ticketdomain.domains.user.domain.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByUserRole(UserRole userRole);
}
