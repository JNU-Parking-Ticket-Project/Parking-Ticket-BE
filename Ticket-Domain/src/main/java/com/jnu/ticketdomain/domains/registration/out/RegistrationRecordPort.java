package com.jnu.ticketdomain.domains.registration.out;


import com.jnu.ticketdomain.domains.events.domain.Sector;
import com.jnu.ticketdomain.domains.registration.domain.Registration;

public interface RegistrationRecordPort {
    Registration save(Registration registration);

    void delete(Registration registration);

    void deleteBySector(Long sectorId);

    void deleteByEvent(Long eventId);
}
