package com.jnu.ticketdomain.domains.registration;

import com.jnu.ticketdomain.domains.events.domain.Event;
import com.jnu.ticketdomain.domains.events.domain.Sector;
import com.jnu.ticketdomain.domains.registration.domain.Registration;
import com.jnu.ticketdomain.domains.registration.repository.RegistrationRepository;
import com.jnu.ticketdomain.domains.user.domain.User;
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
    @DisplayName("성공 : 정렬조건대로 정상조회된다")
    void findSortedRegistrationsByEventId() {
        // given
        Event event = Event.builder().title("이벤트").build();
        em.persist(event);

        Sector sector1 = Sector.builder().sectorNumber("1구간").name("소프트웨어공학과").sectorCapacity(10).reserve(1).build();
        Sector sector2 = Sector.builder().sectorNumber("2구간").name("전자컴퓨터공학과").sectorCapacity(10).reserve(1).build();
        sector1.setEvent(event);
        sector2.setEvent(event);

        em.persist(sector1);
        em.persist(sector2);

        User user1 = UserTestBuilder.builder()
                .withEmail("success1@example.com")
                .asSuccess()
                .build();
        User user2 = UserTestBuilder.builder()
                .withEmail("prepare1@example.com")
                .asPrepare(1)
                .build();
        User user3 = UserTestBuilder.builder()
                .withEmail("success2@example.com")
                .asSuccess()
                .build();

        em.persist(user1);
        em.persist(user2);
        em.persist(user3);

        Registration reg1 = RegistrationTestBuilder.builder()
                .withUser(user1)
                .withSector(sector1)
                .withStudentNum("10001")
                .build();
        Registration reg2 = RegistrationTestBuilder.builder()
                .withUser(user2)
                .withSector(sector1)
                .withStudentNum("10002")
                .build();
        Registration reg3 = RegistrationTestBuilder.builder()
                .withUser(user3)
                .withSector(sector2)
                .withStudentNum("10003")
                .build();

        em.persist(reg1);
        em.persist(reg2);
        em.persist(reg3);
        em.flush();
        em.clear();

        // when
        List<Registration> result = registrationRepository.findSortedRegistrationsByEventId(event.getId());

        // then
        assertThat(result).hasSize(3);

        // sector1의 SUCCESS(user1) → sector1의 PREPARE(user2) → sector2의 SUCCESS(user3)
        assertThat(result.get(0).getUser().getEmail()).isEqualTo("success1@example.com");
        assertThat(result.get(1).getUser().getEmail()).isEqualTo("prepare1@example.com");
        assertThat(result.get(2).getUser().getEmail()).isEqualTo("success2@example.com");
    }

}
