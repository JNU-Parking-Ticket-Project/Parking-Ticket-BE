package com.jnu.ticketapi.api.event.service;

import static com.jnu.ticketcommon.consts.TicketStatic.REDIS_EVENT_ISSUE_STORE;

import com.jnu.ticketcommon.annotation.UseCase;
import com.jnu.ticketdomain.common.domainEvent.Events;
import com.jnu.ticketdomain.domains.events.adaptor.EventAdaptor;
import com.jnu.ticketdomain.domains.events.adaptor.SectorAdaptor;
import com.jnu.ticketdomain.domains.events.domain.Event;
import com.jnu.ticketdomain.domains.events.domain.EventStatus;
import com.jnu.ticketdomain.domains.events.event.EventDeletedEvent;
import com.jnu.ticketdomain.domains.registration.adaptor.RegistrationAdaptor;
import com.jnu.ticketinfrastructure.redis.RedisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class EventDeleteUseCase {
    private final EventAdaptor eventAdaptor;

    @Autowired(required = false)
    private RedisRepository redisRepository;

    private final SectorAdaptor sectorAdaptor;
    private final RegistrationAdaptor registrationAdaptor;

    @Value("${ableRedis:true}")
    private boolean ableRedis;

    @Transactional
    public void deleteEvent(Long eventId) {
        Event event = eventAdaptor.findById(eventId);
        Events.raise(EventDeletedEvent.of(event));
        event.deleteEvent();
        event.updateStatus(EventStatus.CLOSED, null);
        if (ableRedis) {
            redisRepository.delete(REDIS_EVENT_ISSUE_STORE);
        }
        sectorAdaptor.deleteByEvent(eventId);
        registrationAdaptor.deleteByEvent(eventId);
    }
}
