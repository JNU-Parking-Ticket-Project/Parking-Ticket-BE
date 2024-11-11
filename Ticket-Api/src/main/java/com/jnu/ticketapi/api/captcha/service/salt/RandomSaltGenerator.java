package com.jnu.ticketapi.api.captcha.service.salt;


import com.jnu.ticketapi.config.EncryptionProperties;
import java.security.SecureRandom;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "encryption", name = "salt-type", havingValue = "random")
@RequiredArgsConstructor
public class RandomSaltGenerator implements SaltGenerator {
    private static final SecureRandom RANDOM = new SecureRandom();

    private final EncryptionProperties properties;

    @Override
    public byte[] generateSalt() {
        byte[] salt = new byte[properties.getLength()];
        RANDOM.nextBytes(salt);
        return salt;
    }

    @Override
    public byte[] decodeSalt(String encodedSalt) {
        return Base64.getDecoder().decode(encodedSalt);
    }
}
