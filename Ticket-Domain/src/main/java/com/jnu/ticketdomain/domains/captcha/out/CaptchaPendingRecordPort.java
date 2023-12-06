package com.jnu.ticketdomain.domains.captcha.out;


import com.jnu.ticketdomain.domains.captcha.domain.CaptchaPending;

public interface CaptchaPendingRecordPort {
    CaptchaPending save(CaptchaPending captchaPending);
}
