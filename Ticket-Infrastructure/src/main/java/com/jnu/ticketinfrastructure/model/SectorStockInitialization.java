package com.jnu.ticketinfrastructure.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SectorStockInitialization {
    private final String stockKey;
    private final String sequenceKey;
    private final int remainingAmount;
    private final int assignedPosition;
}
