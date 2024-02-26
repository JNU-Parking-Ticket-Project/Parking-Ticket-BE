package com.jnu.ticketapi.api.event.model.request;

import com.jnu.ticketcommon.message.ValidationMessage;
import com.jnu.ticketdomain.common.vo.DateTimePeriod;
import lombok.Builder;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Builder
public record EventUpdateRequest(
        @NotNull(message = "날짜를 " + ValidationMessage.MUST_NOT_NULL) DateTimePeriod dateTimePeriod,
        @NotBlank(message = "제목을 " + ValidationMessage.MUST_NOT_BLANK) String title) {
}
