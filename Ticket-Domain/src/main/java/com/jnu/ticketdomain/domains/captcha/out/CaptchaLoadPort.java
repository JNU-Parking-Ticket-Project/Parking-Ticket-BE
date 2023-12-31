package com.jnu.ticketdomain.domains.captcha.out;


import com.jnu.ticketcommon.annotation.Port;
import com.jnu.ticketdomain.domains.captcha.domain.Captcha;

@Port
public interface CaptchaLoadPort {

    Captcha findByRandom();

    Captcha findById(long id);
}
