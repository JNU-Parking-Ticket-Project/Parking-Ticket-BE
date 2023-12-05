package com.jnu.ticketdomain.domains.council.out;

import com.jnu.ticketcommon.annotation.Port;
import com.jnu.ticketdomain.domains.user.domain.User;

@Port
public interface CouncilLoadPort {
    User findByEmail(String email);
}
