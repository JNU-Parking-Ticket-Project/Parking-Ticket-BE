package com.jnu.ticketdomain.domains.council.out;


import com.jnu.ticketcommon.annotation.Port;
import com.jnu.ticketdomain.domains.council.domain.Council;
import com.jnu.ticketdomain.domains.user.domain.User;
import java.util.List;

@Port
public interface CouncilLoadPort {
    User findByEmail(String email);

    List<Council> findAll();
}
