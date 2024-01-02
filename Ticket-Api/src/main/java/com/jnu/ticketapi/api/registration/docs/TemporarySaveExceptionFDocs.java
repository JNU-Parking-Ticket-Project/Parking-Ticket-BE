package com.jnu.ticketapi.api.registration.docs;


import com.jnu.ticketcommon.annotation.ExceptionDoc;
import com.jnu.ticketcommon.annotation.ExplainError;
import com.jnu.ticketcommon.exception.GlobalErrorCode;
import com.jnu.ticketcommon.exception.TicketCodeException;
import com.jnu.ticketcommon.interfaces.SwaggerExampleExceptions;
import com.jnu.ticketdomain.domains.events.exception.NotFoundSectorException;
import com.jnu.ticketdomain.domains.registration.exception.RegistrationErrorCode;
import com.jnu.ticketdomain.domains.user.exception.NotFoundUserException;

@ExceptionDoc
public class TemporarySaveExceptionFDocs implements SwaggerExampleExceptions {
    @ExplainError("요청하는 사용자를 찾을 수 없는 경우")
    public TicketCodeException 사용자를_찾을_수_없습니다 = NotFoundUserException.EXCEPTION;

    @ExplainError("selectSectorId에 해당 하는 구간을 찾을 수 없는 경우")
    public TicketCodeException 구간을_찾을_수_없습니다 = NotFoundSectorException.EXCEPTION;

    @ExplainError("이름에 null 혹은 빈칸인 경우")
    public TicketCodeException 이름에_null_혹은_빈칸인_경우 = new TicketCodeException(GlobalErrorCode.NAME_MUST_NOT_BLANK);

    @ExplainError("학번에 null 혹은 빈칸인 경우")
    public TicketCodeException 학번에_null_혹은_빈칸인_경우 = new TicketCodeException(GlobalErrorCode.STUDENT_NUMBER_MUST_NOT_BLANK);

    @ExplainError("소속대학에 null 혹은 빈칸인 경우")
    public TicketCodeException 소속대학에_null_혹은_빈칸인_경우 = new TicketCodeException(RegistrationErrorCode.AFFILIATION_MUST_NOT_BLANK);

    @ExplainError("차량번호에 null 혹은 빈칸인 경우")
    public TicketCodeException 차량번호에_null_혹은_빈칸인_경우 = new TicketCodeException(RegistrationErrorCode.CAR_NUMBER_MUST_NOT_BLANK);

    @ExplainError("경차여부에 null인 경우")
    public TicketCodeException 경차여부에_null인_경우 = new TicketCodeException(RegistrationErrorCode.CAR_LIGHT_MUST_NOT_NULL);

    @ExplainError("전화번호 형식이 올바르지 않는 경우")
    public TicketCodeException 전화번호_형식이_올바르지_않는_경우 = new TicketCodeException(GlobalErrorCode.PHONE_NUMBER_NOT_VALID);

    @ExplainError("선택 구간 id가 음수인 경우")
    public TicketCodeException 선택_구간_id가_음수인_경우 = new TicketCodeException(RegistrationErrorCode.SELECT_SECTORID_MUST_POSITIVE_NUMBER);

}
