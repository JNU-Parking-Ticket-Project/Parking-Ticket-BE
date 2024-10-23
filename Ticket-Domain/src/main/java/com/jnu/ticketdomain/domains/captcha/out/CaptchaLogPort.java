package com.jnu.ticketdomain.domains.captcha.out;


import com.jnu.ticketcommon.annotation.Port;
import com.jnu.ticketdomain.domains.captcha.domain.CaptchaLog;

@Port
public interface CaptchaLogPort {
    CaptchaLog save(CaptchaLog entity);

    CaptchaLog findLatestByUserId(Long userId);
}
