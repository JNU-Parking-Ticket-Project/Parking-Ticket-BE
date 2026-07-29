package com.jnu.ticketinfrastructure.model;


import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DeadLetterQueueMessage {
    private final String recordId;
    private final String originalRecordId;
    private final String payload;
    private final int failureCount;
    private final String lastError;
    private final long failedAt;
    private final String reason;
}
