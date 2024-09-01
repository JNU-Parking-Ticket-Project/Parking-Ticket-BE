package com.jnu.ticketinfrastructure.domainEvent;


import com.jnu.ticketinfrastructure.model.ChatMessage;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class EventIssuedEvent extends InfrastructureEvent {
    private ChatMessage message;
    private final Double score;

    public static EventIssuedEvent from(
            ChatMessage message, Double score) {
        return EventIssuedEvent.builder()
                .message(message)
                .score(score)
                .build();
    }
}
