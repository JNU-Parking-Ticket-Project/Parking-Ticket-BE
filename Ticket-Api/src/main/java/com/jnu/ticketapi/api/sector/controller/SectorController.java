package com.jnu.ticketapi.api.sector.controller;

import static com.jnu.ticketcommon.consts.TicketStatic.SECTOR_SUCCESS_REGISTER_MESSAGE;

import com.jnu.ticketapi.api.sector.docs.CreateSectorExceptionDocs;
import com.jnu.ticketapi.api.sector.request.SectorRegisterRequest;
import com.jnu.ticketapi.api.sector.service.SectorRegisterUseCase;
import com.jnu.ticketcommon.annotation.ApiErrorExceptionsExample;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "3. [구간]")
@RequiredArgsConstructor
public class SectorController {
    private final SectorRegisterUseCase sectorRegisterUseCase;

    @Operation(summary = "구간 추가", description = "구간 설정(구간 번호, 구간 이름, 구간별 수용인원, 잔여 인원))")
    @ApiErrorExceptionsExample(CreateSectorExceptionDocs.class)
    @PostMapping("/sector")
    public ResponseEntity<String> setCoupon(@RequestBody List<SectorRegisterRequest> sectors) {
        sectorRegisterUseCase.execute(sectors);
        return ResponseEntity.ok(SECTOR_SUCCESS_REGISTER_MESSAGE);
    }
}
