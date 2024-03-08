package com.jnu.ticketapi.api.sector.service;


import com.jnu.ticketcommon.annotation.UseCase;
import com.jnu.ticketdomain.domains.events.domain.Sector;
import com.jnu.ticketdomain.domains.events.exception.CannotUpdatePublishEventException;
import com.jnu.ticketdomain.domains.events.out.SectorLoadPort;
import com.jnu.ticketdomain.domains.events.out.SectorRecordPort;
import com.jnu.ticketdomain.domains.registration.out.RegistrationRecordPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class SectorDeleteUseCase {
    private final SectorRecordPort sectorRecordPort;
    private final SectorLoadPort sectorLoadPort;
    private final RegistrationRecordPort registrationRecordPort;

    @Transactional
    public void execute(Long sectorId) {
        // to Sector List
        Sector sector = sectorLoadPort.findById(sectorId);
        if (Boolean.TRUE.equals(sector.getEvent().getPublish()))
            throw CannotUpdatePublishEventException.EXCEPTION;
        registrationRecordPort.deleteBySector(sector);
        sectorRecordPort.delete(sector);
    }
}
