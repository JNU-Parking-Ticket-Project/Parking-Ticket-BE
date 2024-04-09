package com.jnu.ticketinfrastructure.domainEvent;


import com.jnu.ticketdomain.domains.registration.domain.Registration;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class EventIssuedEvent extends InfrastructureEvent {
    private final Registration registration;
    private final Long currentUserId;
    private final Long sectorId;

    public static EventIssuedEvent from(
            Registration registration, Long currentUserId, Long sectorId) {
        return EventIssuedEvent.builder()
                .registration(registration)
                .currentUserId(currentUserId)
                .sectorId(sectorId)
                .build();
    }
}
