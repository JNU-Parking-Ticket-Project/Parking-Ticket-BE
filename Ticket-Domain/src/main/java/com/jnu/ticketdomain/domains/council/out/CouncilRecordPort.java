package com.jnu.ticketdomain.domains.council.out;


import com.jnu.ticketcommon.annotation.Port;
import com.jnu.ticketdomain.domains.council.domain.Council;

@Port
public interface CouncilRecordPort {
    Council save(Council council);
}
