package com.jnu.ticketapi.application.helper;

import com.jnu.ticketapi.config.EncryptionProperties;
import com.jnu.ticketcommon.exception.EncryptionErrorException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
@RequiredArgsConstructor
public class Encryption {
    private final EncryptionProperties properties;

    public String encrypt(String data, String iv) {
        try {
            Cipher cipher = Cipher.getInstance(properties.getAlgorithm());
            SecretKeySpec keySpec = new SecretKeySpec(properties.getKey().getBytes(), properties.getKeySpecAlgorithm());
            IvParameterSpec ivParamSpec = new IvParameterSpec(Base64.getDecoder().decode(iv));
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivParamSpec);

            byte[] encrypted = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw EncryptionErrorException.EXCEPTION;
        }
    }

    public String decrypt(String encryptedData, String iv) {
        try {
            Cipher cipher = Cipher.getInstance(properties.getAlgorithm());
            SecretKeySpec keySpec = new SecretKeySpec(properties.getKey().getBytes(), properties.getKeySpecAlgorithm());
            IvParameterSpec ivParamSpec = new IvParameterSpec(Base64.getDecoder().decode(iv));
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivParamSpec);

            byte[] decodedBytes = Base64.getDecoder().decode(encryptedData);
            byte[] decrypted = cipher.doFinal(decodedBytes);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw EncryptionErrorException.EXCEPTION;
        }
    }
}