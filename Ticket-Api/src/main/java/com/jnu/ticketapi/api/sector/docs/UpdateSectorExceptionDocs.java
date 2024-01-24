package com.jnu.ticketapi.api.sector.docs;


import com.jnu.ticketcommon.annotation.ExceptionDoc;
import com.jnu.ticketcommon.annotation.ExplainError;
import com.jnu.ticketcommon.exception.TicketCodeException;
import com.jnu.ticketcommon.interfaces.SwaggerExampleExceptions;
import com.jnu.ticketdomain.domains.events.exception.DuplicateSectorNameException;
import com.jnu.ticketdomain.domains.events.exception.InvalidSectorCapacityAndRemainException;
import com.jnu.ticketdomain.domains.events.exception.InvalidSizeSectorException;
import com.jnu.ticketdomain.domains.events.exception.NotFoundSectorException;

@ExceptionDoc
public class UpdateSectorExceptionDocs implements SwaggerExampleExceptions {

    @ExplainError("구간을 찾을 수 없습니다.")
    public TicketCodeException 구간_찾지_못함 = NotFoundSectorException.EXCEPTION;

    @ExplainError(
            "Sector 생성 검증 기준을 만족하지 못합니다. 1) capacity는 0보다 커야합니다. 2) remain은 capacity보다 작거나 같아야합니다.")
    public TicketCodeException 구간_생성_검증_실패 = InvalidSectorCapacityAndRemainException.EXCEPTION;

    @ExplainError("Sector 생성 검증 기준을 만족하지 못합니다. 2) 구간명(sectorNum)이 중복됩니다.")
    public TicketCodeException 구간명_중복_생성 = DuplicateSectorNameException.EXCEPTION;

    @ExplainError public TicketCodeException 수정_구간_크기_불일치 = InvalidSizeSectorException.EXCEPTION;
}
