package com.jnu.ticketdomain.domains.user.exception;

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
public enum UserErrorCode implements BaseErrorCode {
    NOT_FOUND_USER(NOT_FOUND, "USER_404_1", "존재하지 않는 유저 입니다."),
    @ExplainError("정상적인 인증 링크가 아닙니다.")
    CREDENTIAL_CODE_NOT_EXIST_ERROR(BAD_REQUEST, "USER_400_2", "정상적인 인증 링크가 아닙니다."),
    USER_PHONE_INVALID(BAD_REQUEST, "USER_400_2", "유저의 휴대폰 전화번호가 올바르지않습니다. 관리자에게 문의주세요"),
    @ExplainError("SMS 발송시 보내는 유저의 전화번호 정보가 null이라 SMS 발송 불가 경우")
    USER_PHONE_EMPTY(BAD_REQUEST, "USER_400_3", "유저의 휴대폰 전화번호가 null입니다.");
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
