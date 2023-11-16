package com.jnu.ticketdomain.domains.out;


import com.jnu.ticketdomain.domains.user.domian.User;

public interface UserRecordPort {
    User save(User user);
}
