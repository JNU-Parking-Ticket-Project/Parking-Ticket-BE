package com.jnu.ticketdomain.domains.coupon.exception;

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
public enum SectorErrorCode implements BaseErrorCode {
    DUPLICATE_SECTOR_CODE(BAD_REQUEST, "Sector_400_1", "동일한 구간 코드가 이미 존재합니다."),
    ALREADY_REGISTER_SECTOR(BAD_REQUEST, "Sector_400_4", "이미 발급된 구간입니다."),
    NO_SECTOR_STOCK_LEFT(BAD_REQUEST, "Sector_400_5", "구간이 모두 소진됐습니다."),
    NOT_FOUND_SECTOR(NOT_FOUND, "Sector_404_2", "존재하지 않는 구간 입니다."),
    NOT_MY_SECTOR(BAD_REQUEST, "Sector_400_7", "내 구간이 아닙니다."),
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
