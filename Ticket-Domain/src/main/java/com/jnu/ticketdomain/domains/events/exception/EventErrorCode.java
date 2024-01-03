package com.jnu.ticketdomain.domains.events.exception;

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
    DUPLICATE_EVENT_CODE(BAD_REQUEST, "EVENT_400_1", "동일한 이벤트 코드가 이미 존재합니다."),
    NO_EVENT_STOCK_LEFT(BAD_REQUEST, "EVENT_400_2", "재고의 잔량이 존재하지 않습니다."),
    NOT_EVENT_ISSUING_PERIOD(BAD_REQUEST, "EVENT_400_3", "이벤트 발급 가능 시각이 아닙니다."),
    NOT_EVENT_OPENING_PERIOD(BAD_REQUEST, "EVENT_400_4", "오픈된 이벤트가 없습니다."),
    NOT_MY_EVENT(BAD_REQUEST, "EVENT_400_5", "내 이벤트가 아닙니다."),
    ALREADY_ISSUED_EVENT(BAD_REQUEST, "EVENT_400_6", "이미 발급된 이벤트입니다."),
    ALREADY_USED_EVENT(BAD_REQUEST, "EVENT_400_7", "이미 사용한 이벤트입니다."),
    ALREADY_OPEN_STATUS(BAD_REQUEST, "Event_400_8", "이미 오픈 중인 이벤트입니다."),
    ALREADY_CALCULATING_STATUS(BAD_REQUEST, "Event_400_9", "이미 정산중인 이벤트입니다."),
    ALREADY_CLOSE_STATUS(BAD_REQUEST, "Event_400_10", "이미 닫은 이벤트입니다."),
    ALREADY_PREPARING_STATUS(BAD_REQUEST, "Event_400_11", "이미 준비중인 이벤트입니다."),
    ALREADY_DELETED_STATUS(BAD_REQUEST, "Event_400_12", "이미 삭제된 이벤트입니다."),
    CANNOT_MODIFY_OPEN_EVENT(BAD_REQUEST, "EVENT_400_13", "오픈된 이벤트은 수정할 수 없습니다."),
    NOT_FOUND_EVENT(NOT_FOUND, "EVENT_404_5", "존재하지 않는 이벤트입니다."),
    USE_OTHER_API(BAD_REQUEST, "Event_400_0", "잘못된 접근입니다."),
    NOT_EVENT_READY_STATUS(BAD_REQUEST, "Event_400_14", "이벤트가 READY상태 여야 합니다."),
    ;
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
