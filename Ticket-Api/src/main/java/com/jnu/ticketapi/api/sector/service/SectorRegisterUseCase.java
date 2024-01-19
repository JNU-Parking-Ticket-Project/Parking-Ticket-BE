package com.jnu.ticketapi.api.sector.service;


import com.jnu.ticketapi.api.sector.model.request.SectorRegisterRequest;
import com.jnu.ticketapi.api.sector.model.response.SectorReadResponse;
import com.jnu.ticketcommon.annotation.UseCase;
import com.jnu.ticketcommon.exception.MultiException;
import com.jnu.ticketcommon.exception.TicketCodeException;
import com.jnu.ticketdomain.common.aop.event.EventTypeCheck;
import com.jnu.ticketdomain.domains.events.domain.Event;
import com.jnu.ticketdomain.domains.events.domain.EventStatus;
import com.jnu.ticketdomain.domains.events.domain.Sector;
import com.jnu.ticketdomain.domains.events.exception.DuplicateSectorNameException;
import com.jnu.ticketdomain.domains.events.exception.InvalidSectorCapacityAndRemainException;
import com.jnu.ticketdomain.domains.events.out.EventLoadPort;
import com.jnu.ticketdomain.domains.events.out.SectorLoadPort;
import com.jnu.ticketdomain.domains.events.out.SectorRecordPort;
import io.vavr.collection.Seq;
import io.vavr.control.Validation;
import java.util.LinkedList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class SectorRegisterUseCase {

    private final SectorRecordPort sectorRecordPort;
    private final SectorLoadPort sectorLoadPort;
    private final EventLoadPort eventLoadPort;

    @Transactional
    public void execute(List<SectorRegisterRequest> sectors) {
        Validation<Seq<TicketCodeException>, List<SectorRegisterRequest>> result =
                validateRegistrationSector(sectors);
        Event recentEvent = eventLoadPort.findRecentEvent();
        result.fold(
                errors -> {
                    throw new MultiException(errors);
                },
                validSectors -> {
                    List<Sector> sectorList =
                            sectors.stream()
                                    .map(
                                            sectorRegisterRequest ->
                                                    new Sector(
                                                            sectorRegisterRequest.sectorNumber(),
                                                            sectorRegisterRequest.name(),
                                                            sectorRegisterRequest.sectorCapacity(),
                                                            sectorRegisterRequest.reserve()))
                                    .toList();
                    sectorList.forEach(sector -> sector.setEvent(recentEvent));
                    sectorRecordPort.saveAll(sectorList);
                    return null;
                });
    }

    private Validation<Seq<TicketCodeException>, List<SectorRegisterRequest>>
            validateRegistrationSector(List<SectorRegisterRequest> sectors) {
        return Validation.combine(
                        validateUniqueSectorNumbers(sectors),
                        validatePlusSectorCapacityAndReserve(sectors))
                .ap((uniqueSectorNumbers, plusSectorCapacityAndReserve) -> sectors);
    }

    private Validation<TicketCodeException, io.vavr.collection.List<String>>
            validateUniqueSectorNumbers(java.util.List<SectorRegisterRequest> sectors) {
        java.util.List<String> sectorNumbers = new LinkedList<>();
        sectors.forEach(sector -> sectorNumbers.add(sector.sectorNumber()));

        return sectorNumbers.stream().distinct().count() == sectorNumbers.size()
                ? Validation.valid(io.vavr.collection.List.ofAll(sectorNumbers))
                : Validation.invalid(DuplicateSectorNameException.EXCEPTION);
    }

    private Validation<TicketCodeException, io.vavr.collection.List<SectorRegisterRequest>>
            validatePlusSectorCapacityAndReserve(java.util.List<SectorRegisterRequest> sectors) {
        return sectors.stream()
                        .allMatch(sector -> sector.sectorCapacity() >= 0 && sector.reserve() >= 0)
                ? Validation.valid(io.vavr.collection.List.ofAll(sectors))
                : Validation.invalid(InvalidSectorCapacityAndRemainException.EXCEPTION);
    }

    @Transactional
    @EventTypeCheck(eventType = EventStatus.READY)
    public void update(List<SectorRegisterRequest> sectors) {
        List<Sector> prevSector = sectorLoadPort.findAll();
        List<Sector> sectorList =
                sectors.stream()
                        .map(
                                sectorRegisterRequest ->
                                        new Sector(
                                                sectorRegisterRequest.sectorNumber(),
                                                sectorRegisterRequest.name(),
                                                sectorRegisterRequest.sectorCapacity(),
                                                sectorRegisterRequest.reserve()))
                        .toList();
        sectorRecordPort.updateAll(prevSector, sectorList);
    }

    @Transactional(readOnly = true)
    public List<SectorReadResponse> findAll() {
        List<Sector> all = sectorLoadPort.findAll();
        return SectorReadResponse.toSectorReadResponses(all);
    }
}
