package com.jnu.ticketinfrastructure.domainEvent;


import com.jnu.ticketdomain.domains.registration.domain.Registration;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class EventIssuedEvent extends InfrastructureEvent {
    private final Long sectorId;
    private final Long userId;
    private final Long eventId;

    public static EventIssuedEvent from(Long sectorId, Long userId, Long eventId) {
        return EventIssuedEvent.builder()
                .sectorId(sectorId)
                .userId(userId)
                .eventId(eventId)
                .build();
    }
}
