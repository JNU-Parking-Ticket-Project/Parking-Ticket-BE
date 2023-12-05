package com.jnu.ticketapi.api.user.service;


import com.jnu.ticketapi.api.user.model.response.UpdateRoleResponse;
import com.jnu.ticketapi.application.helper.Converter;
import com.jnu.ticketcommon.annotation.UseCase;
import com.jnu.ticketdomain.domains.user.adaptor.UserAdaptor;
import com.jnu.ticketdomain.domains.user.domain.User;
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

    @Transactional
    public User save(User user) {
        return userAdaptor.save(user);
    }

    @Transactional
    public UpdateRoleResponse updateRole(Long userId, String role) {
        User user = userAdaptor.updateRole(userId, role);
        return converter.toUpdateRoleResponseDto(user);
    }
}
