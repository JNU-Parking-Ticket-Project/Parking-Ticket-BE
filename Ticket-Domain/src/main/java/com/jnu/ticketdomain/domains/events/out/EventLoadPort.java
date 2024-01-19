package com.jnu.ticketdomain.domains.events.out;


import com.jnu.ticketcommon.annotation.Port;
import com.jnu.ticketcommon.utils.Result;
import com.jnu.ticketdomain.domains.events.domain.Event;

@Port
public interface EventLoadPort {
    Result<Event, Object> findReadyEvent();

    Event findById(Long eventId);

    Event findOpenEvent();

    Result<Event, Object> findReadyOrOpenEvent();
}
