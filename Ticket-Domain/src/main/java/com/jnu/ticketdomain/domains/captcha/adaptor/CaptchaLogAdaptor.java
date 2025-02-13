package com.jnu.ticketdomain.domains.captcha.adaptor;


import com.jnu.ticketcommon.annotation.Adaptor;
import com.jnu.ticketdomain.domains.captcha.domain.CaptchaLog;
import com.jnu.ticketdomain.domains.captcha.exception.NotFoundCaptchaLogException;
import com.jnu.ticketdomain.domains.captcha.out.CaptchaLogPort;
import com.jnu.ticketdomain.domains.captcha.repository.CaptchaLogRepository;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Adaptor
public class CaptchaLogAdaptor implements CaptchaLogPort {
    private final CaptchaLogRepository captchaLogRepository;

    @Override
    public CaptchaLog save(CaptchaLog entity) {
        return captchaLogRepository.save(entity);
    }

    @Override
    public CaptchaLog findLatestByUserId(Long userId) {
        return captchaLogRepository.findTopByUserIdAndIsSuccessFalseOrderByTimestampDesc(userId)
                .orElseThrow(() -> NotFoundCaptchaLogException.EXCEPTION);
    }
}
