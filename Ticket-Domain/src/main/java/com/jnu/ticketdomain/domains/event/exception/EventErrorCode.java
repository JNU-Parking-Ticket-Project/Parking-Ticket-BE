package com.jnu.ticketdomain.domains.event.exception;

import static com.jnu.ticketcommon.consts.TicketStatic.BAD_REQUEST;
import static com.jnu.ticketcommon.consts.TicketStatic.NOT_FOUND;

import com.jnu.ticketcommon.annotation.ExplainError;
import com.jnu.ticketcommon.exception.BaseErrorCode;
import com.jnu.ticketcommon.exception.ErrorReason;
import java.lang.reflect.Field;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum EventErrorCode implements BaseErrorCode {
    DUPLICATE_EVENT_CODE(BAD_REQUEST, "EVENT_400_1", "동일한 쿠폰 코드가 이미 존재합니다."),
    ALREADY_ISSUED_EVENT(BAD_REQUEST, "EVENT_400_4", "이미 발급된 쿠폰입니다."),
    NO_EVENT_STOCK_LEFT(BAD_REQUEST, "EVENT_400_5", "쿠폰이 모두 소진됐습니다."),
    NOT_EVENT_ISSUING_PERIOD(BAD_REQUEST, "EVENT_400_6", "쿠폰 발급 가능 시각이 아닙니다."),
    NOT_EVENT_OPENING_PERIOD(BAD_REQUEST, "EVENT_400_9", "오픈된 쿠폰이 없습니다."),
    NOT_FOUND_EVENT(NOT_FOUND, "EVENT_404_2", "존재하지 않는 쿠폰 입니다."),
    NOT_MY_EVENT(BAD_REQUEST, "EVENT_400_7", "내 쿠폰이 아닙니다."),
    ALREADY_USED_EVENT(BAD_REQUEST, "EVENT_400_8", "이미 사용한 쿠폰입니다."),
    CANNOT_MODIFY_OPEN_EVENT(BAD_REQUEST, "EVENT_400_10", "오픈된 이벤트은 수정할 수 없습니다."),
    private final Integer status;
    private final String code;
    private final String reason;

    @Override
    public ErrorReason getErrorReason() {
        return ErrorReason.builder().reason(reason).code(code).status(status).build();
    }

    @Override
    public String getExplainError() throws NoSuchFieldException {
        Field field = this.getClass().getField(this.name());
        ExplainError annotation = field.getAnnotation(ExplainError.class);
        return Objects.nonNull(annotation) ? annotation.value() : this.getReason();
    }
}
