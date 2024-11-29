package com.jnu.ticketapi.api.captcha.service;

import com.jnu.ticketapi.api.captcha.service.vo.HashResult;

public interface CaptchaHashProcessor{
    HashResult hash(Long captchaId);

    Long verify(String hashedCode, Long userId);
}
