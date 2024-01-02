package com.jnu.ticketdomain.domains.registration.exception;

import static com.jnu.ticketcommon.consts.TicketStatic.NOT_FOUND;

import com.jnu.ticketcommon.annotation.ExplainError;
import com.jnu.ticketcommon.exception.BaseErrorCode;
import com.jnu.ticketcommon.exception.ErrorReason;
import java.lang.reflect.Field;
import java.util.Objects;

import com.jnu.ticketcommon.message.ValidationMessage;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum RegistrationErrorCode implements BaseErrorCode {
    NOT_FOUND_REGISTRATION(NOT_FOUND, "REGISTRATION_404_1", "존재하지 않는 등록 신청 입니다."),
    ALREADY_EXIST_REGISTRATION(400, "REGISTRATION_400_1", "이미 등록 신청을 하셨습니다."),
    AFFILIATION_MUST_NOT_BLANK(400, "BAD_REQUEST", "소속대학을 " + ValidationMessage.MUST_NOT_BLANK),
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
