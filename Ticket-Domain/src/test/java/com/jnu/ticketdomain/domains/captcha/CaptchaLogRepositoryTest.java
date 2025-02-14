package com.jnu.ticketdomain.domains.captcha;

import static org.assertj.core.api.Assertions.assertThat;

import com.jnu.ticketdomain.domains.captcha.domain.CaptchaLog;
import com.jnu.ticketdomain.domains.captcha.repository.CaptchaLogRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class CaptchaLogRepositoryTest {

    @Autowired private CaptchaLogRepository captchaLogRepository;

    @BeforeEach
    void setUp() {
        captchaLogRepository.deleteAll();
    }

    @Test
    @DisplayName("유저 ID로 최신 캡차 로그를 성공적으로 조회한다")
    void findLatestByUserId_Success() {
        // given
        Long userId = 1L;
        Long otherUserId = 2L;

        CaptchaLog oldLog =
                CaptchaLog.builder()
                        .userId(userId)
                        .captchaId(1L)
                        .salt("salt1")
                        .isSuccess(false)
                        .build();

        CaptchaLog middleLog =
                CaptchaLog.builder()
                        .userId(userId)
                        .captchaId(2L)
                        .salt("salt2")
                        .isSuccess(false)
                        .build();

        CaptchaLog latestLog =
                CaptchaLog.builder()
                        .userId(userId)
                        .captchaId(3L)
                        .salt("salt3")
                        .isSuccess(false)
                        .build();

        CaptchaLog otherUserLog =
                CaptchaLog.builder()
                        .userId(otherUserId)
                        .captchaId(4L)
                        .salt("salt4")
                        .isSuccess(false)
                        .build();

        CaptchaLog successLog =
                CaptchaLog.builder()
                        .userId(userId)
                        .captchaId(3L)
                        .salt("salt3")
                        .isSuccess(true)
                        .build();

        captchaLogRepository.saveAll(
                List.of(oldLog, middleLog, latestLog, otherUserLog, successLog));

        // when
        CaptchaLog result =
                captchaLogRepository
                        .findTopByUserIdAndIsSuccessFalseOrderByTimestampDesc(userId)
                        .get();

        // then
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getCaptchaId()).isEqualTo(latestLog.getCaptchaId());
        assertThat(result.getSalt()).isEqualTo(latestLog.getSalt());
    }

    @Test
    @DisplayName("존재하지 않는 유저의 캡차 로그 조회시 null을 반환한다")
    void findLatestByUserId_UserNotFound_ReturnsNull() {
        // given
        Long nonExistentUserId = 999L;

        // when
        Optional<CaptchaLog> result =
                captchaLogRepository.findTopByUserIdAndIsSuccessFalseOrderByTimestampDesc(
                        nonExistentUserId);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("유저의 모든 캡차 로그가 성공 상태인 경우 null을 반환한다")
    void findLatestByUserId_AllLogsSuccess_ReturnsNull() {
        // given
        Long userId = 1L;

        CaptchaLog successLog1 =
                CaptchaLog.builder()
                        .userId(userId)
                        .captchaId(1L)
                        .salt("salt1")
                        .isSuccess(true)
                        .build();

        CaptchaLog successLog2 =
                CaptchaLog.builder()
                        .userId(userId)
                        .captchaId(2L)
                        .salt("salt2")
                        .isSuccess(true)
                        .build();

        captchaLogRepository.saveAll(List.of(successLog1, successLog2));

        // when
        Optional<CaptchaLog> result =
                captchaLogRepository.findTopByUserIdAndIsSuccessFalseOrderByTimestampDesc(userId);

        // then
        assertThat(result).isEmpty();
    }
}
