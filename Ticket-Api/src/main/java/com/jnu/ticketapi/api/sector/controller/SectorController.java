package com.jnu.ticketapi.api.sector.controller;

import static com.jnu.ticketcommon.consts.TicketStatic.OK_REQUEST;
import static com.jnu.ticketcommon.message.ResponseMessage.*;

import com.jnu.ticketapi.api.sector.docs.CreateSectorExceptionDocs;
import com.jnu.ticketapi.api.sector.model.request.SectorReadResponse;
import com.jnu.ticketapi.api.sector.model.request.SectorRegisterRequest;
import com.jnu.ticketapi.api.sector.service.SectorDeleteUseCase;
import com.jnu.ticketapi.api.sector.service.SectorRegisterUseCase;
import com.jnu.ticketcommon.annotation.ApiErrorExceptionsExample;
import com.jnu.ticketcommon.dto.SuccessResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(name = "3. [구간]")
@RequiredArgsConstructor
@SecurityRequirement(name = "access-token")
@RequestMapping("/v1")
public class SectorController {
    private final SectorRegisterUseCase sectorRegisterUseCase;
    private final SectorDeleteUseCase sectorDeleteUseCase;

    @Operation(summary = "구간 조회", description = "구간 조회(구간 번호, 구간 이름, 구간별 수용인원, 잔여 인원))")
    @ApiErrorExceptionsExample(CreateSectorExceptionDocs.class)
    @GetMapping("/sectors")
    public List<SectorReadResponse> getCoupon() {
        return sectorRegisterUseCase.findAll();
    }

    @Operation(summary = "구간 추가", description = "구간 설정(구간 번호, 구간 이름, 구간별 수용인원, 잔여 인원))")
    @ApiErrorExceptionsExample(CreateSectorExceptionDocs.class)
    @PostMapping("/sectors")
    public SuccessResponse setCoupon(@Valid @RequestBody List<SectorRegisterRequest> sectors) {
        sectorRegisterUseCase.execute(sectors);
        return new SuccessResponse(OK_REQUEST, SECTOR_SUCCESS_REGISTER_MESSAGE);
    }

    @Operation(summary = "구간 수정", description = "구간 삭제(구간 번호, 구간 이름, 구간별 수용인원, 잔여 인원))")
    @ApiErrorExceptionsExample(CreateSectorExceptionDocs.class)
    @PutMapping("/sectors")
    public SuccessResponse updateCoupon(@RequestBody List<SectorRegisterRequest> sectors) {
        sectorRegisterUseCase.execute(sectors);
        return new SuccessResponse(OK_REQUEST, SECTOR_SUCCESS_UPDATE_MESSAGE);
    }

    @Operation(summary = "구간 삭제", description = "구간 삭제(구간 번호, 구간 이름, 구간별 수용인원, 잔여 인원))")
    @ApiErrorExceptionsExample(CreateSectorExceptionDocs.class)
    @DeleteMapping("/sectors/{sector-id}")
    public SuccessResponse deleteCoupon(@PathVariable("sector-id") Long sectorId) {
        sectorDeleteUseCase.execute(sectorId);
        return new SuccessResponse(OK_REQUEST, SECTOR_SUCCESS_DELETE_MESSAGE);
    }
}
