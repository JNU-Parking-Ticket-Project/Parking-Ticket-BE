package com.jnu.ticketapi.api.sector.service;


import com.jnu.ticketapi.api.sector.model.request.SectorRegisterRequest;
import com.jnu.ticketapi.api.sector.model.response.SectorReadResponse;
import com.jnu.ticketcommon.annotation.UseCase;
import com.jnu.ticketcommon.exception.TicketCodeException;
import com.jnu.ticketdomain.common.aop.event.EventTypeCheck;
import com.jnu.ticketdomain.domains.events.domain.EventStatus;
import com.jnu.ticketdomain.domains.events.domain.Sector;
import com.jnu.ticketdomain.domains.events.exception.DuplicateSectorNameException;
import com.jnu.ticketdomain.domains.events.out.EventLoadPort;
import com.jnu.ticketdomain.domains.events.out.SectorLoadPort;
import com.jnu.ticketdomain.domains.events.out.SectorRecordPort;
import io.vavr.control.Validation;
import java.util.ArrayList;
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

    //    @Transactional
    //    public void execute(List<SectorRegisterRequest> sectors) {
    //        // 구간별 이름 중복 체그
    //        Validation result = validateRegistrationSector(sectors);
    //        List<Sector> sectorList = sectors.stream()
    //                .map(
    //                    sectorRegisterRequest ->
    //                        new Sector(
    //                            sectorRegisterRequest.sectorNumber(),
    //                            sectorRegisterRequest.name(),
    //                            sectorRegisterRequest.sectorCapacity(),
    //                            sectorRegisterRequest.reserve()))
    //                .toList();
    //        sectorRecordPort.saveAll(sectorList);
    //    }
    @Transactional
    public void execute(List<SectorRegisterRequest> sectors) {
        // Check for duplicate sector names
        Validation<TicketCodeException, io.vavr.collection.List<SectorRegisterRequest>> result =
                validateRegistrationSector(sectors);

        result.fold(
                validationError -> {
                    throw validationError;
                },
                validSectors -> {
                    List<Sector> sectorList =
                            validSectors
                                    .map(
                                            sectorRegisterRequest ->
                                                    new Sector(
                                                            sectorRegisterRequest.sectorNumber(),
                                                            sectorRegisterRequest.name(),
                                                            sectorRegisterRequest.sectorCapacity(),
                                                            sectorRegisterRequest.reserve()))
                                    .toJavaList();
                    sectorRecordPort.saveAll(sectorList);
                    return null;
                });
    }

    private Validation<TicketCodeException, io.vavr.collection.List<SectorRegisterRequest>>
            validateRegistrationSector(List<SectorRegisterRequest> sectors) {
        //        return Validation.combine(validateUniqueSectorNumbers(sectors)
        ////            , validateLogic2()
        ////            , validateLogic3()
        //            )
        //            .ap((uniqueSectorNumbers) -> io.vavr.collection.List.ofAll(sectors));
        return validateUniqueSectorNumbers(sectors)
                .flatMap(
                        uniqueSectorNumbers ->
                                Validation.valid(io.vavr.collection.List.ofAll(sectors)));
    }

    private Validation<TicketCodeException, io.vavr.collection.List<String>>
            validateUniqueSectorNumbers(java.util.List<SectorRegisterRequest> sectors) {
        java.util.List<String> sectorNumbers = new ArrayList<>();
        sectors.forEach(sector -> sectorNumbers.add(sector.sectorNumber()));

        return sectorNumbers.stream().distinct().count() == sectorNumbers.size()
                ? Validation.valid(io.vavr.collection.List.ofAll(sectorNumbers))
                : Validation.invalid(DuplicateSectorNameException.EXCEPTION);
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
