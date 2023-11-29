package com.jnu.ticketdomain.out;


import com.jnu.ticketcommon.annotation.Port;
import com.jnu.ticketdomain.domain.system.System;

@Port
public interface SystemRecordPort {
    System save(System system);
}
