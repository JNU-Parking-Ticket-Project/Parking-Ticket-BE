package com.jnu.ticketapi.application.helper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.jnu.ticketapi.config.EncryptionProperties;
import com.jnu.ticketcommon.exception.DecryptionErrorException;
import com.jnu.ticketcommon.exception.EncryptionErrorException;
import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class EncryptionTest {
    private Encryption encryption;
    private EncryptionProperties properties;
    private String validIv;

    @BeforeEach
    void setUp() {
        properties =
                new EncryptionProperties(
                        "12345678901234567890123456789012", // key
                        "AES/CBC/PKCS5Padding", // algorithm
                        "AES", // key-spec-algorithm
                        16L // length
                        );
        encryption = new Encryption(properties);
        // Base64로 인코딩된 16바이트 IV
        validIv = Base64.getEncoder().encodeToString(new byte[16]);
    }

    @Nested
    @DisplayName("암호화 테스트")
    class EncryptTest {
        @Test
        @DisplayName("정상적인 입력값으로 암호화가 성공한다")
        void encrypt_Success() {
            // given
            String data = "test data";

            // when & then
            assertThat(encryption.encrypt(data, validIv)).isNotNull();
        }

        @Test
        @DisplayName("잘못된 IV 길이로 암호화 시 예외가 발생한다")
        void encrypt_WithInvalidIvLength_ThrowsException() {
            // given
            String data = "test data";
            String invalidIv = Base64.getEncoder().encodeToString(new byte[8]); // 8바이트 IV

            // when & then
            assertThrows(EncryptionErrorException.class, () -> encryption.encrypt(data, invalidIv));
        }

        @Test
        @DisplayName("IV가 Base64로 인코딩되지 않은 경우 예외가 발생한다")
        void encrypt_WithNonBase64Iv_ThrowsException() {
            // given
            String data = "test data";
            String invalidIv = "invalid-iv-format";

            // when & then
            assertThrows(EncryptionErrorException.class, () -> encryption.encrypt(data, invalidIv));
        }
    }

    @Nested
    @DisplayName("복호화 테스트")
    class DecryptTest {
        @Test
        @DisplayName("정상적인 입력값으로 암호화/복호화가 성공한다")
        void decrypt_Success() {
            // given
            String originalData = "test data";
            String encryptedData = encryption.encrypt(originalData, validIv);

            // when
            String decryptedData = encryption.decrypt(encryptedData, validIv);

            // then
            assertThat(decryptedData).isEqualTo(originalData);
        }

        @Test
        @DisplayName("잘못된 암호화 데이터로 복호화 시 예외가 발생한다")
        void decrypt_WithInvalidEncryptedData_ThrowsException() {
            // given
            String invalidEncryptedData = "invalid-encrypted-data";

            // when & then
            assertThrows(
                    DecryptionErrorException.class,
                    () -> encryption.decrypt(invalidEncryptedData, validIv));
        }

        @Test
        @DisplayName("IV가 Base64로 인코딩되지 않은 경우 예외가 발생한다")
        void decrypt_WithNonBase64Iv_ThrowsException() {
            // given
            String encryptedData = "some-encrypted-data";
            String invalidIv = "invalid-iv-format";

            // when & then
            assertThrows(
                    DecryptionErrorException.class,
                    () -> encryption.decrypt(encryptedData, invalidIv));
        }
    }
}
