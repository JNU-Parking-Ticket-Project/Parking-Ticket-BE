package com.jnu.ticketdomain.domains.council.out;


import com.jnu.ticketcommon.annotation.Port;
import com.jnu.ticketdomain.domains.council.domain.Council;
import java.util.List;

@Port
public interface CouncilLoadPort {
    List<Council> findAll();
}
