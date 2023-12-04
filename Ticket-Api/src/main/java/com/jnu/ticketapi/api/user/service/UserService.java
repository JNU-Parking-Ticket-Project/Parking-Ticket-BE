package com.jnu.ticketapi.api.user.service;


import com.jnu.ticketapi.api.user.model.response.UpdateRoleResponse;
import com.jnu.ticketapi.application.helper.Converter;
import com.jnu.ticketdomain.domains.user.adaptor.UserAdaptor;
import com.jnu.ticketdomain.domains.user.domain.User;
import java.util.Optional;

import com.jnu.ticketdomain.domains.user.domain.UserRole;
import com.jnu.ticketdomain.domains.user.exception.NotFoundUserException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService implements UserUseCase {
    private final UserAdaptor userAdaptor;
    private final Converter converter;

    @Transactional(readOnly = true)
    @Override
    public Optional<User> findByEmail(String email) {
        return userAdaptor.findByEmail(email);
    }

    @Transactional(readOnly = true)
    @Override
    public Optional<User> findById(Long userId) { return userAdaptor.findById(userId); }

    @Override
    @Transactional
    public User save(User user) {
        return userAdaptor.save(user);
    }

    @Override
    @Transactional
    public UpdateRoleResponse updateRole(Long userId, String role) {
        User user = findById(userId)
                .orElseThrow(
                        () -> NotFoundUserException.EXCEPTION
        );
        user.updateRole(UserRole.valueOf(role));
        return converter.toUpdateRoleResponseDto(user);
    }
}
