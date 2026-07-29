package com.jnu.ticketinfrastructure.model;


import lombok.Getter;

@Getter
public class StreamQueueMessage {
    private final String streamKey;
    private final String recordId;
    private final ChatMessage message;

    public StreamQueueMessage(String recordId, ChatMessage message) {
        this(null, recordId, message);
    }

    public StreamQueueMessage(String streamKey, String recordId, ChatMessage message) {
        this.streamKey = streamKey;
        this.recordId = recordId;
        this.message = message;
    }
}
