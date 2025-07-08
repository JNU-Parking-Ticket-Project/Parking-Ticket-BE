package com.jnu.ticketapi.application.helper;


import com.jnu.ticketapi.config.EncryptionProperties;
import com.jnu.ticketcommon.exception.DecryptionErrorException;
import com.jnu.ticketcommon.exception.EncryptionErrorException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class Encryption {
    private final EncryptionProperties properties;

    public String encrypt(String data, String iv) {
        try {
            Cipher cipher = Cipher.getInstance(properties.getAlgorithm());
            SecretKeySpec keySpec =
                    new SecretKeySpec(
                            properties.getKey().getBytes(), properties.getKeySpecAlgorithm());
            IvParameterSpec ivParamSpec = new IvParameterSpec(Base64.getDecoder().decode(iv));
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivParamSpec);

            byte[] encrypted = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            log.info(e.getMessage(), e);
            throw EncryptionErrorException.EXCEPTION;
        }
    }

    public String decrypt(String encryptedData, String iv) {
        try {
            Cipher cipher = Cipher.getInstance(properties.getAlgorithm());
            SecretKeySpec keySpec =
                    new SecretKeySpec(
                            properties.getKey().getBytes(), properties.getKeySpecAlgorithm());
            IvParameterSpec ivParamSpec = new IvParameterSpec(Base64.getDecoder().decode(iv));
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivParamSpec);

            byte[] decodedBytes = Base64.getDecoder().decode(encryptedData);
            byte[] decrypted = cipher.doFinal(decodedBytes);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.info(e.getMessage(), e);
            throw DecryptionErrorException.EXCEPTION;
        }
    }
}
