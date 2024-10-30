package com.jnu.ticketdomain.domains.captcha.exception;

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
public enum CaptchaErrorCode implements BaseErrorCode {
    NOT_FOUND_CAPTCHA(NOT_FOUND, "CAPTCHA_404_1", "존재하지 않는 캡챠입니다."),
    NOT_FOUND_CAPTCHA_PENDING(NOT_FOUND, "CAPTCHA_404_2", "존재하지 않는 캡챠 인증입니다."),
    WRONG_CAPTCHA_ANSWER(BAD_REQUEST, "CAPTCHA_400_1", "틀린 캡챠 답변입니다."),
    WRONG_CAPTCHA_CODE(BAD_REQUEST, "CAPTCHA_400_2", "올바르지 않은 캡챠 요청입니다");

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
