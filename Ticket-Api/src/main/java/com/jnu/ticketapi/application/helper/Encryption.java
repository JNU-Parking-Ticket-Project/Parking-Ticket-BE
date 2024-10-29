package com.jnu.ticketapi.application.helper;


import com.jnu.ticketcommon.annotation.Helper;
import com.jnu.ticketcommon.exception.DecryptionErrorException;
import com.jnu.ticketcommon.exception.EncryptionErrorException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

@Helper
@Slf4j
public class Encryption {
    @Value("${encryption.algorithm}")
    private String algorithm;

    @Value("${encryption.secret}")
    private String secret;

    private SecretKeySpec secretKeySpec;

    private SecretKeySpec generateKey() {
        try {
            if (secretKeySpec == null) {
                byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
                MessageDigest sha = MessageDigest.getInstance("SHA-256");
                keyBytes = sha.digest(keyBytes);
                keyBytes = Arrays.copyOf(keyBytes, 16); // AES-128을 위한 16바이트 키
                secretKeySpec = new SecretKeySpec(keyBytes, algorithm);
            }
            return secretKeySpec;
        } catch (Exception e) {
            log.error("Error during generateKey", e);
            throw EncryptionErrorException.EXCEPTION;
        }
    }

    public String encrypt(Long value) {
        try {
            Cipher cipher = Cipher.getInstance(algorithm);
            cipher.init(Cipher.ENCRYPT_MODE, generateKey());
            byte[] valueBytes = ByteBuffer.allocate(Long.BYTES).putLong(value).array();
            byte[] encrypted = cipher.doFinal(valueBytes);
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            log.error("Error during encryption", e);
            throw EncryptionErrorException.EXCEPTION;
        }
    }

    public Long decrypt(final String encryptedValue) {
        try {
            Cipher cipher = Cipher.getInstance(algorithm);
            cipher.init(Cipher.DECRYPT_MODE, generateKey());
            byte[] encryptedBytes = Base64.getDecoder().decode(encryptedValue);
            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
            return ByteBuffer.wrap(decryptedBytes).getLong();
        } catch (Exception e) {
            log.error("Error during decryption", e);
            throw DecryptionErrorException.EXCEPTION;
        }
    }
}
