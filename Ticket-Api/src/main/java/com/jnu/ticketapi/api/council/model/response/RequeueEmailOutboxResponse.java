package com.jnu.ticketapi.api.council.model.response;

public record RequeueEmailOutboxResponse(Long outboxId, Long eventId, String status) {

    public static RequeueEmailOutboxResponse pending(Long eventId, Long outboxId) {
        return new RequeueEmailOutboxResponse(outboxId, eventId, "PENDING");
    }
}
