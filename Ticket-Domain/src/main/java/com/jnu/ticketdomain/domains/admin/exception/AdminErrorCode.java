package com.jnu.ticketdomain.domains.admin.exception;

import com.jnu.ticketcommon.annotation.ExplainError;
import com.jnu.ticketcommon.exception.BaseErrorCode;
import com.jnu.ticketcommon.exception.ErrorReason;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.lang.reflect.Field;
import java.util.Objects;

import static com.jnu.ticketcommon.consts.TicketStatic.BAD_REQUEST;

@Getter
@AllArgsConstructor
public enum AdminErrorCode implements BaseErrorCode {
    ALREADY_EXIST_ADMIN(BAD_REQUEST, "ADMIN_400_1", "ADMIN이 이미 존재합니다."),
    NOT_ALLOW_UPDATE_OWN_ROLE(BAD_REQUEST, "ADMIN_400_2", "자신의 권한을 변경할 수 없습니다.");
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

