package com.jnu.ticketdomain.domains.events.out;


import com.jnu.ticketcommon.annotation.Port;
import com.jnu.ticketdomain.domains.events.domain.Event;

@Port
public interface EventRecordPort {
    Event save(Event event);
}
