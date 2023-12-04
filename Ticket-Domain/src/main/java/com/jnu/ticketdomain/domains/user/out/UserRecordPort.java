package com.jnu.ticketdomain.domains.user.out;


import com.jnu.ticketcommon.annotation.Port;
import com.jnu.ticketdomain.domains.user.domain.User;

@Port
public interface UserRecordPort {
    User save(User user);
}
