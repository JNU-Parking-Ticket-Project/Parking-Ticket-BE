package com.jnu.ticketdomain.domains.notice.exception;

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
public enum NoticeErrorCode implements BaseErrorCode {
    @ExplainError("작성된 안내사항이 존재하지 않습니다.")
    NOTICE_NOT_EXIST_ERROR(NOT_FOUND, "NOTICE_404_1", "안내사항이 존재하지 않습니다."),
    @ExplainError("공지사항 내용은 10,000자를 초과할 수 없습니다.")
    NOTICE_CONTENT_EXCEEDS_LIMIT_ERROR(
            BAD_REQUEST, "NOTICE_400_1", "안내사항 내용은 10,000자를 초과할 수 없습니다."),
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
