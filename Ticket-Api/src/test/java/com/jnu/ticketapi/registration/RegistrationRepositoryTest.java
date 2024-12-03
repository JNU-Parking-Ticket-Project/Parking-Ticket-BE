package com.jnu.ticketapi.registration;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.jnu.ticketdomain.domains.registration.repository.RegistrationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

@SpringBootTest
@ActiveProfiles("test")
@EnableAutoConfiguration(exclude = {BatchAutoConfiguration.class})
@Sql("classpath:db/teardown.sql")
class RegistrationRepositoryTest {

    @Autowired private RegistrationRepository registrationRepository;

    @Test
    @DisplayName("findPositionBySavedAt - 올바른 순번 반환")
    void findPositionBySavedAt() {
        // Given
        Long registration1Id = 2L;
        Long registration2Id = 3L;
        Long secotrId = 4L;

        // When: 메서드 호출
        Integer position1 = registrationRepository.findPositionBySavedAt(registration1Id, secotrId);
        Integer position2 = registrationRepository.findPositionBySavedAt(registration2Id, secotrId);

        // Then: 결과 검증
        assertThat(position1).isEqualTo(1); // 첫 번째 등록은 1순위
        assertThat(position2).isEqualTo(2); // 두 번째 등록은 2순위
    }
}
