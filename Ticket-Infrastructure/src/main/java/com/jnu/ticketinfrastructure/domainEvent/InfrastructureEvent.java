package com.jnu.ticketinfrastructure.domainEvent;


import java.time.LocalDateTime;
import lombok.Getter;

@Getter
public class InfrastructureEvent {
    private final LocalDateTime publishAt;

    public InfrastructureEvent() {
        this.publishAt = LocalDateTime.now();
    }
}
