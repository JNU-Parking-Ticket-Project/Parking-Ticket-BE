package com.jnu.ticketapi.api.admin.model.request;


import com.jnu.ticketcommon.annotation.Enum;
import com.jnu.ticketcommon.message.ValidationMessage;
import com.jnu.ticketdomain.domains.user.domain.UserRole;

public record UpdateRoleRequest(
        @Enum(message = ValidationMessage.INVALID_ROLE_VALUE) UserRole role) {}
