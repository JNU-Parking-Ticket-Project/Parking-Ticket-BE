package com.jnu.ticketinfrastructure.model;


import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DeadLetterTransferResult {
    private final int failureCount;
    private final boolean moved;
}
