package com.jnu.ticketapi.api.registration.docs;


import com.jnu.ticketcommon.annotation.ExceptionDoc;
import com.jnu.ticketcommon.annotation.ExplainError;
import com.jnu.ticketcommon.exception.DecryptionErrorException;
import com.jnu.ticketcommon.exception.GlobalErrorCode;
import com.jnu.ticketcommon.exception.TicketCodeException;
import com.jnu.ticketcommon.interfaces.SwaggerExampleExceptions;
import com.jnu.ticketdomain.domains.captcha.exception.WrongCaptchaAnswerException;
import com.jnu.ticketdomain.domains.events.exception.*;
import com.jnu.ticketdomain.domains.registration.exception.RegistrationErrorCode;
import com.jnu.ticketdomain.domains.user.exception.NotFoundUserException;

@ExceptionDoc
public class FinalSaveExceptionDocs implements SwaggerExampleExceptions {
    @ExplainError("captcha code를 복호화 하는 도중 에러가 발생한 경우")
    public TicketCodeException 복호화_도중_에러가_발생하였습니다 = DecryptionErrorException.EXCEPTION;

    @ExplainError("캡챠 정답이 틀린 경우")
    public TicketCodeException 캡챠_정답이_틀렸습니다 = WrongCaptchaAnswerException.EXCEPTION;

    @ExplainError("요청하는 사용자를 찾을 수 없는 경우")
    public TicketCodeException 사용자를_찾을_수_없습니다 = NotFoundUserException.EXCEPTION;

    @ExplainError("selectSectorId에 해당 하는 구간을 찾을 수 없는 경우")
    public TicketCodeException 구간을_찾을_수_없습니다 = NotFoundSectorException.EXCEPTION;

    @ExplainError("설정한 시간이 현재를 포함하고 있을 경우(쿠폰 검증)")
    public TicketCodeException 쿠폰_검증_시작종료시간_현재를_포함 = InvalidPeriodEventException.EXCEPTION;

    @ExplainError("설정한 시간이 이미 지났을 때 발급한 경우(쿠폰 검증)")
    public TicketCodeException 쿠폰_검증_종료시간_지남 = NotIssuingEventPeriodException.EXCEPTION;

    @ExplainError("시작 시간이 종료 시간보다 늦을 때 발급한 경우(쿠폰 검증)")
    public TicketCodeException 쿠폰_검증_시작시간_종료시간_이후 = NotIssuingEventPeriodException.EXCEPTION;

    @ExplainError("발급하지 않은 쿠폰을 조회할 경우(쿠폰 검증)")
    public TicketCodeException 쿠폰_검증_미발급_쿠폰_조회 = NotFoundEventException.EXCEPTION;

    @ExplainError("이름에 null 혹은 빈칸인 경우")
    public TicketCodeException 이름에_null_혹은_빈칸인_경우 =
            new TicketCodeException(GlobalErrorCode.NAME_MUST_NOT_BLANK);

    @ExplainError("학번에 null 혹은 빈칸인 경우")
    public TicketCodeException 학번에_null_혹은_빈칸인_경우 =
            new TicketCodeException(GlobalErrorCode.STUDENT_NUMBER_MUST_NOT_BLANK);

    @ExplainError("소속대학에 null 혹은 빈칸인 경우")
    public TicketCodeException 소속대학에_null_혹은_빈칸인_경우 =
            new TicketCodeException(RegistrationErrorCode.AFFILIATION_MUST_NOT_BLANK);

    @ExplainError("차량번호에 null 혹은 빈칸인 경우")
    public TicketCodeException 차량번호에_null_혹은_빈칸인_경우 =
            new TicketCodeException(RegistrationErrorCode.CAR_NUMBER_MUST_NOT_BLANK);

    @ExplainError("경차여부에 null인 경우")
    public TicketCodeException 경차여부에_null인_경우 =
            new TicketCodeException(RegistrationErrorCode.CAR_LIGHT_MUST_NOT_NULL);

    @ExplainError("전화번호 형식이 올바르지 않는 경우")
    public TicketCodeException 전화번호_형식이_올바르지_않는_경우 =
            new TicketCodeException(GlobalErrorCode.PHONE_NUMBER_NOT_VALID);

    @ExplainError("선택 구간 id가 음수인 경우")
    public TicketCodeException 선택_구간_id가_음수인_경우 =
            new TicketCodeException(RegistrationErrorCode.SELECT_SECTORID_MUST_POSITIVE_NUMBER);

    @ExplainError("캡챠코드에 null 혹은 빈칸인 경우")
    public TicketCodeException 캡챠코드에_null_혹은_빈칸인_경우 =
            new TicketCodeException(RegistrationErrorCode.CAPTCHA_CODE_MUST_NOT_BLANK);

    @ExplainError("캡챠답변에 null 혹은 빈칸인 경우")
    public TicketCodeException 캡챠답변에_null_혹은_빈칸인_경우 =
            new TicketCodeException(RegistrationErrorCode.CAPTCHA_ANSWER_MUST_NOT_BLANK);

    @ExplainError("Event가 Open 상태가 아닌 경우")
    public TicketCodeException Event가_Open_상태가_아닌_경우 =
            new TicketCodeException(EventErrorCode.NOT_EVENT_OPEN_STATUS);

    @ExplainError("Event가 Ready 상태가 아닌 경우")
    public TicketCodeException Event가_Ready_상태가_아닌_경우 =
            new TicketCodeException(EventErrorCode.NOT_EVENT_READY_STATUS);

    @ExplainError("Sector 재고가 없는 경우")
    public TicketCodeException Sector_재고가_없는_경우 =
            new TicketCodeException(EventErrorCode.NO_EVENT_STOCK_LEFT);

    @ExplainError("이벤트가 종료된 상태인 경우")
    public TicketCodeException 이벤트가_종료된_상태 = AlreadyCloseStatusException.EXCEPTION;

    @ExplainError("이벤트가 게시된 경우")
    public TicketCodeException 이벤트가_게시된_경우 = AlreadyPublishedEventException.EXCEPTION;
}
