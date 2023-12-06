package com.jnu.ticketdomain.domains.council.out;


import com.jnu.ticketcommon.annotation.Port;
import com.jnu.ticketdomain.domains.council.domain.Council;
import com.jnu.ticketdomain.domains.user.domain.User;

@Port
public interface CouncilRecordPort {
    User saveUser(User user);

    Council saveCouncil(Council council);
}
