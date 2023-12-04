package com.jnu.ticketapi.api.user.service;


import com.jnu.ticketapi.api.user.model.response.UpdateRoleResponse;
import com.jnu.ticketdomain.domains.user.domain.User;
import com.jnu.ticketdomain.domains.user.domain.UserRole;

import java.util.Optional;

public interface UserUseCase {
    Optional<User> findByEmail(String email);
    Optional<User> findById(Long userId);
    User save(User user);
    UpdateRoleResponse updateRole(Long userId, String role);
}
