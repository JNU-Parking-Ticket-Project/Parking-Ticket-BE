package com.jnu.ticketapi.api.admin.service;


import com.jnu.ticketapi.api.admin.model.response.UpdateRoleResponse;
import com.jnu.ticketcommon.annotation.UseCase;
import com.jnu.ticketdomain.domains.admin.adaptor.AdminAdaptor;
import com.jnu.ticketdomain.domains.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class AdminUseCase {
    private final AdminAdaptor adminAdaptor;

    @Transactional
    public UpdateRoleResponse updateRole(Long userId, String role) {
        User user = adminAdaptor.updateRole(userId, role);
        return UpdateRoleResponse.of(user);
    }
}
