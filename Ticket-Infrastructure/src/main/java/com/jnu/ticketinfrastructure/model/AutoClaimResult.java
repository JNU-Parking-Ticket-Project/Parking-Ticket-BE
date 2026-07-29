package com.jnu.ticketinfrastructure.model;


import java.util.List;

public record AutoClaimResult(String nextStartId, List<RawStreamMessage> messages) {

    public static AutoClaimResult empty() {
        return new AutoClaimResult("0-0", List.of());
    }
}
