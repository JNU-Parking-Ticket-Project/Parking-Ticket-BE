package com.jnu.ticketdomain.domains.council.exception;

import static com.jnu.ticketcommon.consts.TicketStatic.BAD_REQUEST;

import com.jnu.ticketcommon.annotation.ExplainError;
import com.jnu.ticketcommon.exception.BaseErrorCode;
import com.jnu.ticketcommon.exception.ErrorReason;
import java.lang.reflect.Field;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CouncilErrorCode implements BaseErrorCode {
    ALREADY_EXIST_EMAIL(BAD_REQUEST, "COUNCIL_400_1", "이미 존재하는 이메일 입니다."),
    IS_NOT_COUNCIL(BAD_REQUEST, "COUNCIL_400_2", "권한이 학생회가 아닙니다."),
    FAILED_TO_SEND_EMAIL(BAD_REQUEST,"COUNCIL_400_3" , "이메일 발송에 실패했습니다.");
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
