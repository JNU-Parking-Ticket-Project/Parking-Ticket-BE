package com.jnu.ticketapi.api.captcha.service.salt;

public interface SaltGenerator {
    byte[] generateSalt();

    byte[] decodeSalt(String encodedSalt);
}
