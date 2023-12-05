package com.jnu.ticketdomain.domains.captcha.exception;

import com.jnu.ticketcommon.annotation.ExplainError;
import com.jnu.ticketcommon.exception.BaseErrorCode;
import com.jnu.ticketcommon.exception.ErrorReason;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.lang.reflect.Field;
import java.util.Objects;

import static com.jnu.ticketcommon.consts.TicketStatic.NOT_FOUND;

@Getter
@AllArgsConstructor
public enum CaptchaErrorCode implements BaseErrorCode {
    NOT_FOUND_CAPTCHA(NOT_FOUND, "Captcha_404_1", "존재하지 않는 캡챠입니다.");

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
