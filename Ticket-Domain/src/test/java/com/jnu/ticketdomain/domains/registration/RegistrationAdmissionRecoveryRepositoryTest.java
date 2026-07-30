package com.jnu.ticketdomain.domains.registration;

import static org.assertj.core.api.Assertions.assertThat;

import com.jnu.ticketdomain.domains.events.domain.Event;
import com.jnu.ticketdomain.domains.events.domain.Sector;
import com.jnu.ticketdomain.domains.registration.domain.Registration;
import com.jnu.ticketdomain.domains.registration.repository.RegistrationRepository;
import com.jnu.ticketdomain.domains.user.domain.User;
import com.jnu.ticketdomain.domains.user.domain.UserStatus;
import com.jnu.ticketdomain.fixture.RegistrationTestBuilder;
import com.jnu.ticketdomain.fixture.UserTestBuilder;
import java.util.List;
import javax.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class RegistrationAdmissionRecoveryRepositoryTest {

    @Autowired private RegistrationRepository registrationRepository;
    @Autowired private EntityManager entityManager;

    @Test
    @DisplayName("Redis 복구 조회는 해당 이벤트의 저장 완료·미삭제 신청 전체를 반환한다")
    void findsOnlySavedActiveRegistrationsForEvent() {
        Event event = Event.builder().title("복구 이벤트").build();
        Event otherEvent = Event.builder().title("다른 이벤트").build();
        entityManager.persist(event);
        entityManager.persist(otherEvent);

        Sector sector = sector("1구간", "공과대학", event);
        Sector otherSector = sector("2구간", "자연과학대학", otherEvent);
        entityManager.persist(sector);
        entityManager.persist(otherSector);

        User user = UserTestBuilder.builder().withEmail("recovery@example.com").build();
        entityManager.persist(user);

        Registration success =
                registration(user, sector, event.getId(), "success@example.com", "30001", false);
        success.finalSave(1, UserStatus.SUCCESS, -2);
        Registration failed =
                registration(user, sector, event.getId(), "failed@example.com", "30002", false);
        failed.finalSave(2, UserStatus.FAIL, -1);
        Registration unsaved =
                registration(user, sector, event.getId(), "unsaved@example.com", "30003", true);
        Registration deleted =
                registration(user, sector, event.getId(), "deleted@example.com", "30004", false);
        deleted.finalSave(3, UserStatus.PREPARE, 1);
        deleted.updateIsDeleted(true);
        Registration other =
                registration(
                        user, otherSector, otherEvent.getId(), "other@example.com", "30005", false);
        other.finalSave(1, UserStatus.SUCCESS, -2);

        entityManager.persist(success);
        entityManager.persist(failed);
        entityManager.persist(unsaved);
        entityManager.persist(deleted);
        entityManager.persist(other);
        entityManager.flush();
        entityManager.clear();

        List<Registration> result =
                registrationRepository.findSavedForAdmissionRecovery(event.getId());

        assertThat(result)
                .extracting(Registration::getEmail)
                .containsExactly("success@example.com", "failed@example.com");
        assertThat(result)
                .extracting(Registration::getResultStatus)
                .containsExactly(UserStatus.SUCCESS, UserStatus.FAIL);
    }

    private Sector sector(String sectorNumber, String name, Event event) {
        Sector sector =
                Sector.builder()
                        .sectorNumber(sectorNumber)
                        .name(name)
                        .sectorCapacity(2)
                        .reserve(1)
                        .build();
        sector.setEvent(event);
        return sector;
    }

    private Registration registration(
            User user,
            Sector sector,
            Long eventId,
            String email,
            String studentNumber,
            boolean unsaved) {
        RegistrationTestBuilder builder =
                RegistrationTestBuilder.builder()
                        .withUser(user)
                        .withSector(sector)
                        .withEmail(email)
                        .withStudentNum(studentNumber)
                        .withEventId(eventId);
        if (unsaved) {
            builder.asUnsaved();
        }
        return builder.build();
    }
}
