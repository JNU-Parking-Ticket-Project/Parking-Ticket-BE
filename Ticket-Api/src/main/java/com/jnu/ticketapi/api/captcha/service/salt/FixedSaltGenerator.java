package com.jnu.ticketapi.api.captcha.service.salt;


import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "encryption", name = "salt-type", havingValue = "fixed")
public class FixedSaltGenerator implements SaltGenerator {
    private static final String FIXED_SALT = "fixedSalt";

    @Override
    public byte[] generateSalt() {
        return FIXED_SALT.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public byte[] decodeSalt(String encodedSalt) {
        return Base64.getDecoder().decode(encodedSalt);
    }
}
