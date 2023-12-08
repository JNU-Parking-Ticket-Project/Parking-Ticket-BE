package com.jnu.ticketapi.api.captcha.service;


import com.jnu.ticketcommon.annotation.UseCase;
import com.jnu.ticketdomain.domains.captcha.adaptor.CaptchaPendingAdaptor;
import com.jnu.ticketdomain.domains.captcha.domain.CaptchaPending;
import com.jnu.ticketdomain.domains.captcha.exception.WrongCaptchaAnswerException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class ValidateCaptchaPendingUseCase {

    private final CaptchaPendingAdaptor captchaPendingAdaptor;

    @Transactional
    public void execute(long captchaPendingId, int answer) {
        CaptchaPending captchaPending = captchaPendingAdaptor.findById(captchaPendingId);

        if (captchaPending.validate(answer)) {
            captchaPending.confirm();
        } else {
            throw WrongCaptchaAnswerException.EXCEPTION;
        }
    }
}
