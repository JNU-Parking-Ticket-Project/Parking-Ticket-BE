package com.jnu.ticketdomain.out;


import com.jnu.ticketcommon.annotation.Port;
import com.jnu.ticketdomain.domain.user.User;

@Port
public interface UserRecordPort {
    User save(User user);
}
