package com.jnu.ticketdomain.domains.council.out;


import com.jnu.ticketdomain.domains.user.domain.User;

public interface CouncilLoadPort {
    User findByEmail(String email);
}
