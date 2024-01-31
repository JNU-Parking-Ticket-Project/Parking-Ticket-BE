package com.jnu.ticketapi.api.sector.service;


import com.jnu.ticketapi.api.sector.model.request.SectorRegisterRequest;
import com.jnu.ticketcommon.annotation.UseCase;
import com.jnu.ticketcommon.exception.MultiException;
import com.jnu.ticketcommon.exception.TicketCodeException;
import com.jnu.ticketcommon.utils.Result;
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

        Event recentEvent = eventLoadPort.findRecentEvent();
        List<Sector> prevSector = sectorLoadPort.findByEventId(recentEvent.getId());
        Validation<Seq<TicketCodeException>, List<SectorRegisterRequest>> result =
                validateRegistrationSector(sectors, prevSector);
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
                    List<Sector> savedSectors = sectorRecordPort.saveAll(sectorList);
                    recentEvent.setSector(savedSectors);
                    return null;
                });
    }

    private Validation<Seq<TicketCodeException>, List<SectorRegisterRequest>>
            validateRegistrationSector(
                    List<SectorRegisterRequest> sectors, List<Sector> prevSector) {
        return Validation.combine(
                        validateUniqueSectorNumbers(sectors),
                        validatePlusSectorCapacityAndReserve(sectors),
                        validateDuplicateSectorNum(sectors, prevSector))
                .ap(
                        (uniqueSectorNumbers, plusSectorCapacityAndReserve, duplicatedSector) ->
                                sectors);
    }

    private Validation<Seq<TicketCodeException>, List<SectorRegisterRequest>> validateUpdateSector(
            List<SectorRegisterRequest> sectors) {
        return Validation.combine(
                        validateUniqueSectorNumbers(sectors),
                        validatePlusSectorCapacityAndReserve(sectors))
                .ap((uniqueSectorNumbers, plusSectorCapacityAndReserve) -> sectors);
    }

    /** 단, emptyList는 valid로 처리한다. */
    private Validation<TicketCodeException, List<SectorRegisterRequest>> validateDuplicateSectorNum(
            List<SectorRegisterRequest> sectors, List<Sector> prevSector) {
        if (sectors.isEmpty()) {
            return Validation.valid(sectors);
        }
        // 기존 DB에 저장된 sector와 중복되는 sector가 있는지 확인한다.
        boolean hasDuplicate =
                sectors.stream()
                        .anyMatch(
                                sector ->
                                        prevSector.stream()
                                                .anyMatch(
                                                        prev ->
                                                                prev.getSectorNumber()
                                                                        .equals(
                                                                                sector
                                                                                        .sectorNumber())));
        if (hasDuplicate) {
            throw DuplicateSectorNameException.EXCEPTION;
        }
        return Validation.valid(sectors);
        /*        return Match(sectors)
        .of(
                Case(
                        $(),
                        sectorList -> {
                            boolean hasDuplicate =
                                    sectorList.stream()
                                            .anyMatch(
                                                    sector ->
                                                            prevSector.stream()
                                                                    .anyMatch(
                                                                            prev ->
                                                                                    prev.getSectorNumber()
                                                                                            .equals(
                                                                                                    sector
                                                                                                            .sectorNumber())));

                            return hasDuplicate
                                    ? Validation.invalid(
                                            DuplicateSectorNameException.EXCEPTION)
                                    : Validation.valid(sectorList);
                        }),
                Case($(), Validation::valid));*/
    }

    private Validation<TicketCodeException, io.vavr.collection.List<String>>
            validateUniqueSectorNumbers(java.util.List<SectorRegisterRequest> sectors) {
        java.util.List<String> sectorNumbers = new LinkedList<>();
        sectors.forEach(sector -> sectorNumbers.add(sector.sectorNumber()));
        if (sectorNumbers.stream().distinct().count() == sectorNumbers.size()) {
            return Validation.valid(io.vavr.collection.List.ofAll(sectorNumbers));
        } else {
            throw DuplicateSectorNameException.EXCEPTION;
        }
        /*return sectorNumbers.stream().distinct().count() == sectorNumbers.size()
        ? Validation.valid(io.vavr.collection.List.ofAll(sectorNumbers))
        : Validation.invalid(DuplicateSectorNameException.EXCEPTION);*/
    }

    private Validation<TicketCodeException, io.vavr.collection.List<SectorRegisterRequest>>
            validatePlusSectorCapacityAndReserve(java.util.List<SectorRegisterRequest> sectors) {
        sectors.forEach(
                sector -> {
                    if (sector.sectorCapacity() < 0 || sector.reserve() < 0) {
                        throw InvalidSectorCapacityAndRemainException.EXCEPTION;
                    }
                });
        return Validation.valid(io.vavr.collection.List.ofAll(sectors));
        /*        return sectors.stream()
                .allMatch(sector -> sector.sectorCapacity() >= 0 && sector.reserve() >= 0)
        ? Validation.valid(io.vavr.collection.List.ofAll(sectors))
        : Validation.invalid(InvalidSectorCapacityAndRemainException.EXCEPTION);*/
    }

    @Transactional
    @EventTypeCheck(eventType = EventStatus.READY)
    public void update(List<SectorRegisterRequest> sectors) {
        Validation<Seq<TicketCodeException>, List<SectorRegisterRequest>> result =
                validateUpdateSector(sectors);
        result.fold(
                errors -> {
                    throw new MultiException(errors);
                },
                validSectors -> {
                    List<Sector> prevSector = sectorLoadPort.findAll();
                    Result<Event, Object> readyOrOpenEvent = eventLoadPort.findReadyOrOpenEvent();
                    Event event = readyOrOpenEvent.getOrThrow();
                    List<Sector> sectorStream =
                            prevSector.stream()
                                    .filter(sector -> sector.getEvent().getId() == event.getId())
                                    .toList();
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
                    sectorRecordPort.updateAll(sectorStream, sectorList);
                    return null;
                });
    }
}
