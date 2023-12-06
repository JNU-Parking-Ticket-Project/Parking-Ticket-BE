package com.jnu.ticketdomain.domains.admin.out;


import com.jnu.ticketdomain.domains.user.domain.User;

public interface AdminRecordPort {
    User updateRole(Long userId, String role);
}
