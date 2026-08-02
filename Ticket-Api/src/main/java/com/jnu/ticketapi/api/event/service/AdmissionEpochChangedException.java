package com.jnu.ticketapi.api.event.service;

class AdmissionEpochChangedException extends RuntimeException {
    AdmissionEpochChangedException(Long eventId, Long journalId) {
        super(
                "Redis admission epoch changed before decision commit. eventId="
                        + eventId
                        + ", journalId="
                        + journalId);
    }
}
