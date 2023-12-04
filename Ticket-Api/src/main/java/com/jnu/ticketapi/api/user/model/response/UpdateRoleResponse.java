package com.jnu.ticketapi.api.user.model.response;

import lombok.Builder;

@Builder
public record UpdateRoleResponse(String role, Long userId) {
}
