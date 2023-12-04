package com.jnu.ticketapi.api.user.service;


import com.jnu.ticketapi.api.user.model.response.UpdateRoleResponse;
import com.jnu.ticketapi.application.helper.Converter;
import com.jnu.ticketcommon.annotation.UseCase;
import com.jnu.ticketdomain.domains.user.adaptor.UserAdaptor;
import com.jnu.ticketdomain.domains.user.domain.User;
import com.jnu.ticketdomain.domains.user.domain.UserRole;
import com.jnu.ticketdomain.domains.user.exception.NotFoundUserException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@UseCase
public class UserUseCase {
    private final UserAdaptor userAdaptor;
    private final Converter converter;

    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return userAdaptor.findByEmail(email);
    }

    @Transactional(readOnly = true)
    public Optional<User> findById(Long userId) {
        return userAdaptor.findById(userId);
    }

    @Transactional
    public User save(User user) {
        return userAdaptor.save(user);
    }

    @Transactional
    public UpdateRoleResponse updateRole(Long userId, String role) {
        User user = findById(userId).orElseThrow(() -> NotFoundUserException.EXCEPTION);
        user.updateRole(UserRole.valueOf(role));
        return converter.toUpdateRoleResponseDto(user);
    }
}
