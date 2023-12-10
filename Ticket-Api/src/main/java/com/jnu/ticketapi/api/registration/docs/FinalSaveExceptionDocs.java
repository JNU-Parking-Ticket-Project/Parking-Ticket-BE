package com.jnu.ticketapi.api.registration.docs;


import com.jnu.ticketcommon.annotation.ExceptionDoc;
import com.jnu.ticketcommon.annotation.ExplainError;
import com.jnu.ticketcommon.exception.DecryptionErrorException;
import com.jnu.ticketcommon.exception.TicketCodeException;
import com.jnu.ticketcommon.interfaces.SwaggerExampleExceptions;
import com.jnu.ticketdomain.domains.captcha.exception.WrongCaptchaAnswerException;
import com.jnu.ticketdomain.domains.coupon.exception.InvalidPeriodCouponException;
import com.jnu.ticketdomain.domains.coupon.exception.NotFoundCouponException;
import com.jnu.ticketdomain.domains.coupon.exception.NotFoundSectorException;
import com.jnu.ticketdomain.domains.coupon.exception.NotIssuingCouponPeriodException;
import com.jnu.ticketdomain.domains.user.exception.NotFoundUserException;

@ExceptionDoc
public class FinalSaveExceptionDocs implements SwaggerExampleExceptions {
    @ExplainError("captchaPending code를 복호화 하는 도중 에러가 발생한 경우")
    public TicketCodeException 복호화_도중_에러가_발생하였습니다 = DecryptionErrorException.EXCEPTION;

    @ExplainError("캡챠 정답이 틀린 경우")
    public TicketCodeException 캡챠_정답이_틀렸습니다 = WrongCaptchaAnswerException.EXCEPTION;
    @ExplainError("요청하는 사용자를 찾을 수 없는 경우")
    public TicketCodeException 사용자를_찾을_수_없습니다 = NotFoundUserException.EXCEPTION;
    @ExplainError("selectSectorId에 해당 하는 구간을 찾을 수 없는 경우")
    public TicketCodeException 구간을_찾을_수_없습니다 = NotFoundSectorException.EXCEPTION;
    @ExplainError("설정한 시간이 현재를 포함하고 있을 경우(쿠폰 검증)")
    public TicketCodeException 쿠폰_검증_시작종료시간_현재를_포함 = InvalidPeriodCouponException.EXCEPTION;

    @ExplainError("설정한 시간이 이미 지났을 때 발급한 경우(쿠폰 검증)")
    public TicketCodeException 쿠폰_검증_종료시간_지남 = NotIssuingCouponPeriodException.EXCEPTION;

    @ExplainError("시작 시간이 종료 시간보다 늦을 때 발급한 경우(쿠폰 검증)")
    public TicketCodeException 쿠폰_검증_시작시간_종료시간_이후 = NotIssuingCouponPeriodException.EXCEPTION;

    @ExplainError("발급하지 않은 쿠폰을 조회할 경우(쿠폰 검증)")
    public TicketCodeException 쿠폰_검증_미발급_쿠폰_조회 = NotFoundCouponException.EXCEPTION;
}
