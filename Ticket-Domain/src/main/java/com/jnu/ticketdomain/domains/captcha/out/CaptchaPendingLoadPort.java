package com.jnu.ticketdomain.domains.captcha.out;

import com.jnu.ticketcommon.annotation.Port;
import com.jnu.ticketdomain.domains.captcha.domain.CaptchaPending;

@Port
public interface CaptchaPendingLoadPort {
    CaptchaPending findById(Long id);
}
