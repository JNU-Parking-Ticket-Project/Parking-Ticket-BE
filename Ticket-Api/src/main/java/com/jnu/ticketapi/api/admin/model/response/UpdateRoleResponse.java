package com.jnu.ticketapi.api.admin.model.response;


import com.jnu.ticketdomain.domains.user.domain.User;
import lombok.Builder;

@Builder
public record UpdateRoleResponse(String role, Long userId) {
    public static UpdateRoleResponse of(User user) {
        return UpdateRoleResponse.builder()
                .userId(user.getId())
                .role(user.getUserRole().toString())
                .build();
    }
}
