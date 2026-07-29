package com.jnu.ticketdomain.domains.registration;

import com.jnu.ticketdomain.domains.events.domain.Event;
import com.jnu.ticketdomain.domains.events.domain.Sector;
import com.jnu.ticketdomain.domains.registration.domain.Registration;
import com.jnu.ticketdomain.domains.registration.repository.RegistrationRepository;
import com.jnu.ticketdomain.domains.user.domain.User;
import com.jnu.ticketdomain.domains.user.domain.UserStatus;
import com.jnu.ticketdomain.fixture.RegistrationTestBuilder;
import com.jnu.ticketdomain.fixture.UserTestBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import javax.persistence.EntityManager;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class RegistrationRepositoryTest {

    @Autowired
    private RegistrationRepository registrationRepository;

    @Autowired
    private EntityManager em;

    @Test
    @DisplayName("과거 이벤트 신청 목록은 현재 User 상태가 아닌 Registration 결과로 조회된다")
    void findSortedRegistrationsByEventId() {
        Event pastEvent = Event.builder().title("과거 이벤트").build();
        Event currentEvent = Event.builder().title("현재 이벤트").build();
        em.persist(pastEvent);
        em.persist(currentEvent);

        Sector pastSector = Sector.builder()
                .sectorNumber("1구간")
                .name("소프트웨어공학과")
                .sectorCapacity(1)
                .reserve(1)
                .build();
        Sector currentSector = Sector.builder()
                .sectorNumber("1구간")
                .name("전자컴퓨터공학과")
                .sectorCapacity(1)
                .reserve(0)
                .build();
        pastSector.setEvent(pastEvent);
        currentSector.setEvent(currentEvent);
        em.persist(pastSector);
        em.persist(currentSector);

        User laterFailedUser = UserTestBuilder.builder()
                .withEmail("past-success@example.com")
                .asFail()
                .build();
        User laterSucceededUser = UserTestBuilder.builder()
                .withEmail("past-prepare@example.com")
                .asSuccess()
                .build();
        User failedRegistrationUser = UserTestBuilder.builder()
                .withEmail("past-fail@example.com")
                .asSuccess()
                .build();
        em.persist(laterFailedUser);
        em.persist(laterSucceededUser);
        em.persist(failedRegistrationUser);

        Registration pastSuccess = RegistrationTestBuilder.builder()
                .withUser(laterFailedUser)
                .withSector(pastSector)
                .withEmail("past-success@example.com")
                .withStudentNum("10001")
                .withEventId(pastEvent.getId())
                .withSavedAt(1_000L)
                .withResult(1, UserStatus.SUCCESS, -2)
                .build();
        Registration pastPrepare = RegistrationTestBuilder.builder()
                .withUser(laterSucceededUser)
                .withSector(pastSector)
                .withEmail("past-prepare@example.com")
                .withStudentNum("10002")
                .withEventId(pastEvent.getId())
                .withSavedAt(2_000L)
                .withResult(2, UserStatus.PREPARE, 1)
                .build();
        Registration pastFail = RegistrationTestBuilder.builder()
                .withUser(failedRegistrationUser)
                .withSector(pastSector)
                .withEmail("past-fail@example.com")
                .withStudentNum("10003")
                .withEventId(pastEvent.getId())
                .withSavedAt(3_000L)
                .withResult(3, UserStatus.FAIL, -1)
                .build();
        Registration currentFailure = RegistrationTestBuilder.builder()
                .withUser(laterFailedUser)
                .withSector(currentSector)
                .withEmail("past-success@example.com")
                .withStudentNum("10001")
                .withEventId(currentEvent.getId())
                .withSavedAt(4_000L)
                .withResult(1, UserStatus.FAIL, -1)
                .build();
        em.persist(pastSuccess);
        em.persist(pastPrepare);
        em.persist(pastFail);
        em.persist(currentFailure);
        em.flush();
        em.clear();

        List<Registration> result =
                registrationRepository.findSortedRegistrationsByEventId(pastEvent.getId());

        assertThat(result)
                .extracting(Registration::getEmail)
                .containsExactly("past-success@example.com", "past-prepare@example.com");
        assertThat(result)
                .extracting(Registration::getResultStatus)
                .containsExactly(UserStatus.SUCCESS, UserStatus.PREPARE);
        assertThat(result).extracting(Registration::getPosition).containsExactly(1, 2);
    }
}
