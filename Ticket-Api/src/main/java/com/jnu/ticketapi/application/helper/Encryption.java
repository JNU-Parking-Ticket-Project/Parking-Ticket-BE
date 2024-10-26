package com.jnu.ticketapi.application.helper;


import com.jnu.ticketapi.application.HashResult;
import com.jnu.ticketcommon.annotation.Helper;
import com.jnu.ticketcommon.exception.EncryptionErrorException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

@Helper
public class Encryption {

    private static final int SALT_LENGTH = 16;
    private static final String HASH_ALGORITHM = "SHA-256";
    private static final SecureRandom RANDOM = new SecureRandom();

    public HashResult encrypt(final Long plainText) {
        byte[] salt = generateSalt();
        return encryptWithSaltBytes(plainText, salt);
    }

    public boolean validateCaptchaId(String encryptedCode, Long captchaId, String captchaSalt) {
        HashResult result = encryptWithSalt(captchaId, captchaSalt);
        return encryptedCode.equals(result.getCaptchaCode());
    }

    private byte[] generateSalt() {
        byte[] salt = new byte[SALT_LENGTH];
        RANDOM.nextBytes(salt);
        return salt;
    }

    private HashResult encryptWithSaltBytes(final Long plainText, final byte[] salt) {
        try {
            byte[] hash = computeHash(plainText.toString().getBytes(StandardCharsets.UTF_8), salt);

            return new HashResult(
                    Base64.getEncoder().encodeToString(salt),
                    Base64.getEncoder().encodeToString(hash));
        } catch (NoSuchAlgorithmException e) {
            throw EncryptionErrorException.EXCEPTION;
        }
    }

    private byte[] computeHash(byte[] data, byte[] salt) throws NoSuchAlgorithmException {
        byte[] saltedData = combineSaltAndData(salt, data);
        return calculateHash(saltedData);
    }

    private byte[] combineSaltAndData(byte[] salt, byte[] data) {
        byte[] combined = new byte[salt.length + data.length];
        System.arraycopy(salt, 0, combined, 0, salt.length);
        System.arraycopy(data, 0, combined, salt.length, data.length);
        return combined;
    }

    private byte[] calculateHash(byte[] data) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
        return digest.digest(data);
    }

    private HashResult encryptWithSalt(final Long plainText, final String encodedSalt) {
        byte[] salt = Base64.getDecoder().decode(encodedSalt);
        return encryptWithSaltBytes(plainText, salt);
    }
}
