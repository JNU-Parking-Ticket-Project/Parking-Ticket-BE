package com.jnu.ticketinfrastructure.domainEvent;


import com.jnu.ticketinfrastructure.model.ChatMessage;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class EventIssuedEvent extends InfrastructureEvent {
    private ChatMessage message;
    private final Double score;
    private final String streamRecordId;

    public static EventIssuedEvent from(ChatMessage message, Double score) {
        return EventIssuedEvent.builder().message(message).score(score).build();
    }

    public static EventIssuedEvent from(ChatMessage message, String streamRecordId) {
        return EventIssuedEvent.builder().message(message).streamRecordId(streamRecordId).build();
    }

    @Override
    public String toString() {
        return "EventIssuedEvent{"
                + "message="
                + message
                + ", score="
                + score
                + ", streamRecordId='"
                + streamRecordId
                + '\''
                + '}';
    }
}
