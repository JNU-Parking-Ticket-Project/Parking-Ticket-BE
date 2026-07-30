package com.jnu.ticketinfrastructure.admission;

public interface RegistrationAdmissionFallbackGateway {
    void activateDatabaseFallback(Long eventId, Throwable cause);
}
