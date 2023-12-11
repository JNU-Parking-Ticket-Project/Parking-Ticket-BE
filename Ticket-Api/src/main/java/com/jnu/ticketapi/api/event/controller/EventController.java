package com.jnu.ticketapi.api.event.controller;

import static com.jnu.ticketcommon.message.ResponseMessage.EVENT_SUCCESS_OPEN_MESSAGE;
import static com.jnu.ticketcommon.message.ResponseMessage.EVENT_SUCCESS_REGISTER_MESSAGE;

import com.jnu.ticketapi.api.event.docs.CreateEventExceptionDocs;
import com.jnu.ticketapi.api.event.docs.ReadEventExceptionDocs;
import com.jnu.ticketapi.api.event.model.request.UpdateEventStatusRequest;
import com.jnu.ticketapi.api.event.service.EventRegisterUseCase;
import com.jnu.ticketapi.api.event.service.EventWithDrawUseCase;
import com.jnu.ticketapi.api.event.service.OpenEventUseCase;
import com.jnu.ticketapi.api.event.service.UpdateEventStatusUseCase;
import com.jnu.ticketcommon.annotation.ApiErrorExceptionsExample;
import com.jnu.ticketcommon.dto.SuccessResponse;
import com.jnu.ticketdomain.common.vo.DateTimePeriod;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "2. [쿠폰]")
@RequiredArgsConstructor
@SecurityRequirement(name = "access-token")
@RequestMapping("/v1")
public class EventController {

    private final EventRegisterUseCase EventRegisterUseCase;
    private final EventWithDrawUseCase EventWithDrawUseCase;
    private final UpdateEventStatusUseCase updateEventStatusUseCase;
    private final OpenEventUseCase openEventUseCase;

    @Operation(summary = "주차권 설정", description = "주차권 행사 세부 설정(시작일, 종료일, 잔고)")
    @ApiErrorExceptionsExample(CreateEventExceptionDocs.class)
    @PostMapping("/events")
    public SuccessResponse setEvent(@RequestBody DateTimePeriod dateTimePeriod) {
        EventRegisterUseCase.registerEvent(dateTimePeriod);
        return new SuccessResponse(EVENT_SUCCESS_REGISTER_MESSAGE);
    }

    @Operation(summary = "주차권 신청", description = "주차권 신청(주차권 신청시 잔고 감소)")
    @Deprecated(since = "2023-12-08", forRemoval = true)
    @PostMapping("/events/apply")
    public SuccessResponse issueEvent() {
        //        EventWithDrawUseCase.issueEvent();
        return new SuccessResponse(EVENT_SUCCESS_REGISTER_MESSAGE);
    }

    @Operation(summary = "현재 대기번호 조회", description = "주차권 대기번호 조회")
    @ApiErrorExceptionsExample(ReadEventExceptionDocs.class)
    @GetMapping("/events/order")
    public ResponseEntity<Long> getEventOrder() {
        return ResponseEntity.ok(EventWithDrawUseCase.getEventOrder());
    }

    @Operation(summary = "이벤트를 오픈 상태로 변경합니다.")
    @PostMapping("events/{event-id}/open")
    public SuccessResponse openEventStatus() {
        openEventUseCase.execute();
        return new SuccessResponse(EVENT_SUCCESS_OPEN_MESSAGE);
    }

    @Operation(summary = "이벤트를 상태를 변경합니다.")
    @PutMapping("events/{event-id}/status")
    public SuccessResponse updateEventStatus(
            @PathVariable Long eventId,
            @RequestBody @Valid UpdateEventStatusRequest updateEventDetailRequest) {
        updateEventStatusUseCase.execute(eventId, updateEventDetailRequest);
        return new SuccessResponse(EVENT_SUCCESS_OPEN_MESSAGE);
    }

    @Operation(summary = "주차권 신청 기간 조회", description = "주차권 신청 기간 조회")
    @ApiErrorExceptionsExample(ReadEventExceptionDocs.class)
    @GetMapping("/events/period")
    public ResponseEntity<DateTimePeriod> getEventPeriod() {
        return ResponseEntity.ok(EventWithDrawUseCase.getEventPeriod());
    }
}
