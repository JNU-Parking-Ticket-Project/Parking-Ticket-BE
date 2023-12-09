package com.jnu.ticketapi.api.admin.service;


import com.jnu.ticketapi.api.admin.model.response.GetUsersResponse;
import com.jnu.ticketapi.api.admin.model.response.UpdateRoleResponse;
import com.jnu.ticketcommon.annotation.UseCase;
import com.jnu.ticketdomain.domains.admin.adaptor.AdminAdaptor;
import com.jnu.ticketdomain.domains.council.adaptor.CouncilAdaptor;
import com.jnu.ticketdomain.domains.council.domain.Council;
import com.jnu.ticketdomain.domains.user.domain.User;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class AdminUseCase {
    private final AdminAdaptor adminAdaptor;
    private final CouncilAdaptor councilAdaptor;

    @Transactional
    public UpdateRoleResponse updateRole(Long userId, String role) {
        User user = adminAdaptor.updateRole(userId, role);
        return UpdateRoleResponse.from(user);
    }

    public GetUsersResponse getUserList() {
        List<Council> councilList = councilAdaptor.findAll();
        return GetUsersResponse.of(councilList);
    }
}
