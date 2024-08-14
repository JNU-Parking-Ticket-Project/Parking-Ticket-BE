package com.jnu.ticketapi.api.event.controller;

import static com.jnu.ticketcommon.message.ResponseMessage.*;

import com.jnu.ticketapi.api.event.docs.*;
import com.jnu.ticketapi.api.event.model.request.EventRegisterRequest;
import com.jnu.ticketapi.api.event.model.request.EventUpdateRequest;
import com.jnu.ticketapi.api.event.model.request.UpdateEventPublishRequest;
import com.jnu.ticketapi.api.event.model.request.UpdateEventStatusRequest;
import com.jnu.ticketapi.api.event.model.response.EventDetailResponse;
import com.jnu.ticketapi.api.event.model.response.EventsPagingResponse;
import com.jnu.ticketapi.api.event.model.response.GetEventPeriodResponse;
import com.jnu.ticketapi.api.event.model.response.PublishStatusResponse;
import com.jnu.ticketapi.api.event.service.*;
import com.jnu.ticketcommon.annotation.ApiErrorExceptionsExample;
import com.jnu.ticketcommon.dto.SuccessResponse;
import com.jnu.ticketdomain.common.vo.DateTimePeriod;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    private final GetEventDetailUseCase getEventDetailUseCase;
    private final GetEventsUseCase getEventsUseCase;
    private final GetPublishStatusUseCase getPublishStatusUseCase;
    private final UpdatePublishStatusUseCase updatePublishStatusUseCase;
    private final EventDeleteUseCase eventDeleteUseCase;
    private final EventUpdateUseCase eventUpdateUseCase;

    @Operation(summary = "주차권 설정", description = "주차권 행사 세부 설정(시작일, 종료일, 잔고)")
    @ApiErrorExceptionsExample(CreateEventExceptionDocs.class)
    @PostMapping("/events")
    public SuccessResponse setEvent(@RequestBody @Valid EventRegisterRequest eventRegisterRequest) {
        EventRegisterUseCase.registerEvent(eventRegisterRequest);
        return new SuccessResponse(EVENT_SUCCESS_REGISTER_MESSAGE);
    }

    @Operation(summary = "주차권 수정", description = "주차권 행사 세부 수정(시작일, 종료일, 제목)")
    @ApiErrorExceptionsExample(UpdateEventExceptionDocs.class)
    @PutMapping("/events/{event-id}")
    public SuccessResponse updateEvent(
            @RequestBody @Valid EventUpdateRequest eventUpdateRequest,
            @PathVariable("event-id") Long eventId) {
        eventUpdateUseCase.updateEvent(eventUpdateRequest, eventId);
        return new SuccessResponse(EVENT_SUCCESS_UPDATE_MESSAGE);
    }

    @Operation(summary = "주차권 리셋", description = "현재 활성화 및 대기중인 이벤트를")
    @DeleteMapping("/events/reset")
    public SuccessResponse resetEvent() {
        EventWithDrawUseCase.resetEvent();
        return new SuccessResponse(EVENT_SUCCESS_DELETE_MESSAGE);
    }

    @Operation(summary = "주차권 신청", description = "주차권 신청(주차권 신청시 잔고 감소)")
    @Deprecated(since = "2023-12-08", forRemoval = true)
    @PostMapping("/events/apply")
    public SuccessResponse issueEvent() {
        return new SuccessResponse(EVENT_SUCCESS_REGISTER_MESSAGE);
    }

//    @Operation(summary = "현재 대기번호 조회", description = "주차권 대기번호 조회")
//    @ApiErrorExceptionsExample(ReadEventExceptionDocs.class)
//    @GetMapping("/events/order")
//    public ResponseEntity<Long> getEventOrder() {
//        return ResponseEntity.ok(EventWithDrawUseCase.getEventOrder());
//    }

    @Operation(summary = "이벤트를 오픈 상태로 변경합니다.")
    @PostMapping("events/open")
    public SuccessResponse openEventStatus() {
        openEventUseCase.execute();
        return new SuccessResponse(EVENT_SUCCESS_OPEN_MESSAGE);
    }

    @Operation(
            summary = "이벤트를 상태를 변경합니다.",
            description = "이벤트를 상태를 변경합니다.(READY, OPEN, CALCULATING, CLOSED)")
    @PutMapping("/events/{event-id}/status")
    public SuccessResponse updateEventStatus(
            @PathVariable(name = "event-id") Long eventId,
            @RequestBody @Valid UpdateEventStatusRequest updateEventDetailRequest) {
        updateEventStatusUseCase.execute(eventId, updateEventDetailRequest);
        return new SuccessResponse(EVENT_SUCCESS_UPDATE_STATUS_MESSAGE);
    }

    @Operation(summary = "주차권 신청 기간 조회", description = "주차권 신청 기간 조회")
    @ApiErrorExceptionsExample(ReadEventPeriodExceptionDocs.class)
    @GetMapping("/events/period")
    public ResponseEntity<GetEventPeriodResponse> getEventPeriod() {
        return ResponseEntity.ok(EventWithDrawUseCase.getEventPeriod());
    }

    @Operation(summary = "event별 주차권 신청 기간 조회", description = "event별 주차권 신청 기간 조회")
    @ApiErrorExceptionsExample(ReadEventPeriodExceptionDocs.class)
    @GetMapping("/events/{event-id}/period")
    public ResponseEntity<DateTimePeriod> getEventPeriod(@PathVariable("event-id") Long eventId) {
        return ResponseEntity.ok(EventWithDrawUseCase.getEventPeriodByEventId(eventId));
    }

    @Operation(
            summary = "이벤트 목록 조회",
            description = "이벤트 목록 조회. 제목, 상태, 기간을 response한다. 페이지네이션(페이지 번호, 페이지 개수, 정렬)")
    @GetMapping("/events")
    public ResponseEntity<EventsPagingResponse> getEvents(
            @PageableDefault(
                            sort = {"id"},
                            direction = Sort.Direction.DESC)
                    Pageable pageable) {
        return ResponseEntity.ok(getEventsUseCase.execute(pageable));
    }

    @Operation(
            summary = "이벤트 상세 조회",
            description = "이벤트 상세 조회 -> 구간, 이벤트 제목, 이벤트 상태, 이벤트 기간 응답을 한다,")
    @GetMapping("/events/{event-id}")
    public ResponseEntity<EventDetailResponse> getEventDetail(
            @PathVariable("event-id") Long eventId) {
        return ResponseEntity.ok(getEventDetailUseCase.execute(eventId));
    }

    @Operation(
            summary = "이벤트의 PUBLISH 상태 조회",
            description = "이벤트의 PUBLISH 상태를 조회한다. path variable 은 Long타입인 event-id를 받는다.")
    @GetMapping("/events/publish/{event-id}")
    public ResponseEntity<PublishStatusResponse> getPublish(
            @PathVariable("event-id") Long eventId) {
        return ResponseEntity.ok(getPublishStatusUseCase.execute(eventId));
    }

    @Operation(
            summary = "이벤트의 PUBLISH 상태 변경",
            description =
                    "이벤트의 PUBLISH 상태를 변경한다. path variable은 Long타입인 event-id를 받고 body에는 publish를 받는다.")
    @ApiErrorExceptionsExample(UpdatePublishStatusExceptionDocs.class)
    @PutMapping("/events/publish/{event-id}")
    public SuccessResponse setPublish(
            @PathVariable("event-id") Long eventId,
            @RequestBody UpdateEventPublishRequest request) {
        return updatePublishStatusUseCase.execute(eventId, request.publish());
    }

    @Operation(summary = "이벤트 삭제", description = "이벤트를 삭제한다. path variable은 Long타입인 event-id를 받는다.")
    @ApiErrorExceptionsExample(DeleteEventExceptionDocs.class)
    @DeleteMapping("/events/{event-id}")
    public SuccessResponse deleteEvent(@PathVariable("event-id") Long eventId) {
        eventDeleteUseCase.deleteEvent(eventId);
        return new SuccessResponse(EVENT_SUCCESS_DELETE_MESSAGE);
    }
}
