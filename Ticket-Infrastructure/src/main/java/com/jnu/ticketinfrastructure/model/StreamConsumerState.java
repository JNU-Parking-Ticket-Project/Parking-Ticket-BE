package com.jnu.ticketinfrastructure.model;

public record StreamConsumerState(long lag, long pending, int inFlight) {

    public boolean isDrained() {
        return lag == 0 && pending == 0 && inFlight == 0;
    }
}
