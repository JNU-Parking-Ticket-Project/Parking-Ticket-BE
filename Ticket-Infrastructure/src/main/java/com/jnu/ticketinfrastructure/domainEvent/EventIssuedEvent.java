package com.jnu.ticketinfrastructure.domainEvent;


import com.jnu.ticketdomain.domains.registration.domain.Registration;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class EventIssuedEvent extends InfrastructureEvent {
    private final Long sectorId;
    private final Long userId;

    public static EventIssuedEvent from(Long sectorId, Long userId) {
        return EventIssuedEvent.builder()
                .sectorId(sectorId)
                .userId(userId)
                .build();
    }
}
