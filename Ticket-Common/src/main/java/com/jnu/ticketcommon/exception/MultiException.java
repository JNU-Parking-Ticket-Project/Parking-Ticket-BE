package com.jnu.ticketcommon.exception;


import io.vavr.collection.Seq;

public class MultiException extends RuntimeException {
    private final Seq<TicketCodeException> causes;

    public MultiException(Seq<TicketCodeException> causes) {
        super("Multiple validation errors occurred");
        this.causes = causes;
    }

    public Seq<TicketCodeException> getCauses() {
        return causes;
    }
}
