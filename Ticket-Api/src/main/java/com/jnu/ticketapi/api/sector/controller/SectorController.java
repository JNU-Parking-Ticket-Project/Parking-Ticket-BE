package com.jnu.ticketapi.api.sector.controller;

import static com.jnu.ticketcommon.message.ResponseMessage.SECTOR_SUCCESS_DELETE_MESSAGE;
import static com.jnu.ticketcommon.message.ResponseMessage.SECTOR_SUCCESS_REGISTER_MESSAGE;
import static com.jnu.ticketcommon.message.ResponseMessage.SECTOR_SUCCESS_UPDATE_MESSAGE;

import com.jnu.ticketapi.api.sector.docs.CreateSectorExceptionDocs;
import com.jnu.ticketapi.api.sector.docs.ReadSectorExceptionDocs;
import com.jnu.ticketapi.api.sector.docs.UpdateSectorExceptionDocs;
import com.jnu.ticketapi.api.sector.model.request.SectorRegisterRequest;
import com.jnu.ticketapi.api.sector.model.response.SectorReadResponse;
import com.jnu.ticketapi.api.sector.service.SectorDeleteUseCase;
import com.jnu.ticketapi.api.sector.service.SectorReadUseCase;
import com.jnu.ticketapi.api.sector.service.SectorRegisterUseCase;
import com.jnu.ticketcommon.annotation.ApiErrorExceptionsExample;
import com.jnu.ticketcommon.dto.SuccessResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "3. [구간]")
@RequiredArgsConstructor
@SecurityRequirement(name = "access-token")
@RequestMapping("/v1")
public class SectorController {

    private final SectorRegisterUseCase sectorRegisterUseCase;
    private final SectorDeleteUseCase sectorDeleteUseCase;
    private final SectorReadUseCase sectorReadUseCase;

    @Operation(summary = "구간 조회", description = "구간 조회(구간 번호, 구간 이름, 구간별 수용인원, 잔여 인원))")
    @ApiErrorExceptionsExample(ReadSectorExceptionDocs.class)
    @GetMapping("/sectors")
    public List<SectorReadResponse> getEvent() {
        return sectorReadUseCase.findAllByEventStatus();
    }

    @Operation(
            summary = "eventId로 구간 조회",
            description = "eventId로 구간 조회(구간 번호, 구간 이름, 구간별 수용인원, 잔여 인원))")
    @ApiErrorExceptionsExample(ReadSectorExceptionDocs.class)
    @GetMapping("/events/{event-id}/sectors")
    public List<SectorReadResponse> getEvent(@PathVariable("event-id") Long eventId) {
        return sectorReadUseCase.findByEventId(eventId);
    }

    @Operation(
            summary = "최근 활성화된 Sector 조회",
            description = "최근 활성화된 Sector 조회(구간 번호, 구간 이름, 구간별 수용인원, 잔여 인원))")
    @ApiErrorExceptionsExample(ReadSectorExceptionDocs.class)
    @GetMapping("/sectors/recent")
    public List<SectorReadResponse> getRecentSector() {
        return sectorReadUseCase.findRecentSector();
    }

    @Operation(
            summary = "구간 추가",
            description =
                    "구간 설정(구간 번호, 구간 이름, 구간별 수용인원, 잔여 인원)) \\n 구간 생성시 재고는 0 이상, 중복 구간명 생성이 불가합니다. 예외를 확인해주세요.")
    @ApiErrorExceptionsExample(CreateSectorExceptionDocs.class)
    @PostMapping("/sectors")
    public SuccessResponse setEvent(@RequestBody List<@Valid SectorRegisterRequest> sectors) {
        sectorRegisterUseCase.execute(sectors);
        return new SuccessResponse(SECTOR_SUCCESS_REGISTER_MESSAGE);
    }

    @Operation(
            summary = "구간 수정",
            description = "구간 수정 -> 변경된 구간만 변경이 됩니다. (구간 번호, 구간 이름, 구간별 수용인원, 잔여 인원))")
    @ApiErrorExceptionsExample(UpdateSectorExceptionDocs.class)
    @PutMapping("/sectors")
    public SuccessResponse updateEvent(@RequestBody List<@Valid SectorRegisterRequest> sectors) {
        sectorRegisterUseCase.update(sectors);
        return new SuccessResponse(SECTOR_SUCCESS_UPDATE_MESSAGE);
    }

    @Operation(summary = "구간 삭제", description = "구간 삭제(구간 번호, 구간 이름, 구간별 수용인원, 잔여 인원))")
    @ApiErrorExceptionsExample(CreateSectorExceptionDocs.class)
    @DeleteMapping("/sectors/{sector-id}")
    public SuccessResponse deleteEvent(@PathVariable("sector-id") Long sectorId) {
        sectorDeleteUseCase.execute(sectorId);
        return new SuccessResponse(SECTOR_SUCCESS_DELETE_MESSAGE);
    }
}
