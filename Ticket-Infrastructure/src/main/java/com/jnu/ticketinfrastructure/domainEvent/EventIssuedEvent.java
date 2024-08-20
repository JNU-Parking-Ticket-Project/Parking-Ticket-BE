package com.jnu.ticketinfrastructure.domainEvent;


import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class EventIssuedEvent extends InfrastructureEvent {
    private final Long sectorId;
    private final Long userId;
    private final Long eventId;
    private final String registration;
    private final Double score;

    public static EventIssuedEvent from(Long sectorId, Long userId, Long eventId, String registration, Double score) {
        return EventIssuedEvent.builder()
                .sectorId(sectorId)
                .userId(userId)
                .eventId(eventId)
                .registration(registration)
                .score(score)
                .build();
    }
}
