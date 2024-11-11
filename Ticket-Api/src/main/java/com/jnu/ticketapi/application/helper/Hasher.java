package com.jnu.ticketapi.application.helper;


import com.jnu.ticketapi.config.EncryptionProperties;
import com.jnu.ticketcommon.annotation.Helper;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import lombok.RequiredArgsConstructor;

@Helper
@RequiredArgsConstructor
public class Hasher {
    private final EncryptionProperties properties;

    public byte[] hash(byte[] data) throws NoSuchAlgorithmException {
        return MessageDigest.getInstance(properties.getAlgorithm()).digest(data);
    }

    public boolean verify(String encodedHash, String data) {
        return encodedHash.equals(data);
    }
}
