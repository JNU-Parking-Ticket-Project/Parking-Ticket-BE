package com.jnu.ticketapi.api.captcha.service;


import com.jnu.ticketapi.api.captcha.service.salt.SaltGenerator;
import com.jnu.ticketapi.api.captcha.service.vo.HashResult;
import com.jnu.ticketapi.application.helper.Hasher;
import com.jnu.ticketcommon.exception.EncryptionErrorException;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CaptchaHashProcessor {
    private final SaltGenerator saltGenerator;
    private final Hasher hasher;

    public HashResult hash(Long captchaId) {
        byte[] salt = saltGenerator.generateSalt();
        return getHash(captchaId, salt);
    }

    public boolean verify(String encodedHash, Long captchaId, String encodedSalt) {
        HashResult result = getHash(captchaId, saltGenerator.decodeSalt(encodedSalt));
        return hasher.verify(encodedHash, result.getCaptchaCode());
    }

    private HashResult getHash(Long captchaId, byte[] salt) {
        byte[] hash = createHash(captchaId.toString().getBytes(StandardCharsets.UTF_8), salt);
        return new HashResult(
                Base64.getEncoder().encodeToString(salt), Base64.getEncoder().encodeToString(hash));
    }

    private byte[] createHash(byte[] data, byte[] salt) {
        byte[] combined = new byte[salt.length + data.length];
        System.arraycopy(salt, 0, combined, 0, salt.length);
        System.arraycopy(data, 0, combined, salt.length, data.length);

        try {
            return hasher.hash(combined);
        } catch (NoSuchAlgorithmException e) {
            throw EncryptionErrorException.EXCEPTION;
        }
    }
}
