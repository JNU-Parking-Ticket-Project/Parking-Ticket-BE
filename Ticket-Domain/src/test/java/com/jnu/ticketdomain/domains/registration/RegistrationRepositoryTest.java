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
import org.springframework.data.domain.PageRequest;

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

    @Test
    @DisplayName("수동 메일 대상 신청은 페이지가 바뀌어도 id 순서를 유지한다")
    void findSavedRegistrationsByPageInStableOrder() {
        Event event = Event.builder().title("종료 이벤트").build();
        em.persist(event);

        Sector sector = Sector.builder()
                .sectorNumber("1구간")
                .name("공과대학")
                .sectorCapacity(2)
                .reserve(1)
                .build();
        sector.setEvent(event);
        em.persist(sector);

        User user = UserTestBuilder.builder().withEmail("mail-target@example.com").build();
        em.persist(user);

        Registration first = RegistrationTestBuilder.builder()
                .withUser(user)
                .withSector(sector)
                .withEmail("first@example.com")
                .withStudentNum("20001")
                .withEventId(event.getId())
                .build();
        Registration second = RegistrationTestBuilder.builder()
                .withUser(user)
                .withSector(sector)
                .withEmail("second@example.com")
                .withStudentNum("20002")
                .withEventId(event.getId())
                .build();
        Registration third = RegistrationTestBuilder.builder()
                .withUser(user)
                .withSector(sector)
                .withEmail("third@example.com")
                .withStudentNum("20003")
                .withEventId(event.getId())
                .build();
        em.persist(first);
        em.persist(second);
        em.persist(third);
        em.flush();
        em.clear();

        var firstPage = registrationRepository.findByIsDeletedFalseAndIsSavedTrueByPage(
                event.getId(), PageRequest.of(0, 2));
        var secondPage = registrationRepository.findByIsDeletedFalseAndIsSavedTrueByPage(
                event.getId(), PageRequest.of(1, 2));

        assertThat(firstPage.getContent())
                .extracting(Registration::getId)
                .containsExactly(first.getId(), second.getId());
        assertThat(secondPage.getContent())
                .extracting(Registration::getId)
                .containsExactly(third.getId());
    }
}
