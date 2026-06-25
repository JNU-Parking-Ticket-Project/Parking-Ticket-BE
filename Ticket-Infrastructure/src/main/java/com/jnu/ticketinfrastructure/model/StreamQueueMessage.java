package com.jnu.ticketinfrastructure.model;


import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StreamQueueMessage {
    private final String recordId;
    private final ChatMessage message;
}
