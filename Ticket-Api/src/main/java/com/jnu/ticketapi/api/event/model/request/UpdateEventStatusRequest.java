package com.jnu.ticketapi.api.event.model.request;


import com.jnu.ticketcommon.annotation.Enum;
import com.jnu.ticketdomain.domains.events.domain.EventStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class UpdateEventStatusRequest {
    @Schema(defaultValue = "READY", description = "준비 상태")
    @Enum(message = "올바른 값을 입력해주세요.")
    private EventStatus status;
}
