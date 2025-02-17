package com.jnu.ticketdomain.domains.captcha;

import static org.assertj.core.api.Assertions.assertThat;

import com.jnu.ticketdomain.domains.captcha.domain.Captcha;
import com.jnu.ticketdomain.domains.captcha.repository.CaptchaRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class CaptchaRepositoryTest {

    @Autowired private CaptchaRepository captchaRepository;

    @BeforeEach
    void setUp() {
        captchaRepository.deleteAll();
    }

    @Test
    @DisplayName("fixed 환경에서 사용할 캡챠 조회 쿼리는 저장된 가장 첫번째 캡챠를 조회한다.")
    void findFirstCaptchaTest() {
        // given
        Captcha first = Captcha.builder().answer("answer1").imageName("image1").build();

        Captcha second = Captcha.builder().answer("answer2").imageName("image2").build();

        Captcha third = Captcha.builder().answer("answer3").imageName("image3").build();

        captchaRepository.saveAll(List.of(first, second, third));

        // when
        Captcha result = captchaRepository.findFirstByOrderByIdAsc().orElseThrow();

        // then
        assertThat(result).isEqualTo(first);
        assertThat(result.getAnswer()).isEqualTo("answer1");
        assertThat(result.getImageName()).isEqualTo("image1");
    }
}
