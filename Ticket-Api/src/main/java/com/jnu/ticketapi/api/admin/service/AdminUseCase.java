package com.jnu.ticketapi.api.admin.service;


import com.jnu.ticketapi.api.admin.model.response.GetUsersResponse;
import com.jnu.ticketapi.api.admin.model.response.UpdateRoleResponse;
import com.jnu.ticketapi.config.SecurityUtils;
import com.jnu.ticketcommon.annotation.UseCase;
import com.jnu.ticketdomain.domains.admin.exception.NotAllowUpdateOwnRoleException;
import com.jnu.ticketdomain.domains.council.adaptor.CouncilAdaptor;
import com.jnu.ticketdomain.domains.council.domain.Council;
import com.jnu.ticketdomain.domains.user.adaptor.UserAdaptor;
import com.jnu.ticketdomain.domains.user.domain.User;
import com.jnu.ticketdomain.domains.user.domain.UserRole;
import com.jnu.ticketdomain.domains.admin.exception.AlreadyExistAdminException;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@UseCase
@RequiredArgsConstructor
public class AdminUseCase {
    private final UserAdaptor userAdaptor;
    private final CouncilAdaptor councilAdaptor;

    @Transactional
    public UpdateRoleResponse updateRole(Long userId, String role) {
        if(Objects.equals(role, "ADMIN") && (userAdaptor.exsitsByUserRole("ADMIN"))) {
            throw AlreadyExistAdminException.EXCEPTION;
        }
        if(Objects.equals(userId, SecurityUtils.getCurrentUserId())) {
            throw NotAllowUpdateOwnRoleException.EXCEPTION;
        }
        User user = userAdaptor.findById(userId);
        user.updateRole(UserRole.valueOf(role));
        return UpdateRoleResponse.from(user);
    }

    public GetUsersResponse getUserList() {
        List<Council> councilList = councilAdaptor.findAll();
        return GetUsersResponse.of(councilList);
    }
}
