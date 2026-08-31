package com.jnu.ticketapi.api.event.event;

public record DatabaseFallbackActivatedEvent(Long eventId, long admissionEpoch, String cause) {

    public static DatabaseFallbackActivatedEvent of(
            Long eventId, long admissionEpoch, Throwable cause) {
        if (cause == null) {
            return new DatabaseFallbackActivatedEvent(eventId, admissionEpoch, "원인 정보 없음");
        }
        String message = cause.getMessage() == null ? "메시지 없음" : cause.getMessage();
        return new DatabaseFallbackActivatedEvent(
                eventId, admissionEpoch, cause.getClass().getSimpleName() + ": " + message);
    }
}
