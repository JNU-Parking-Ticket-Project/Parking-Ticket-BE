package com.jnu.ticketapi.api.captcha.service.vo;


import lombok.Getter;

@Getter
public class HashResult {
    private final String salt;
    private final String captchaCode;

    public HashResult(String salt, String captchaCode) {
        this.salt = salt;
        this.captchaCode = captchaCode;
    }
}
