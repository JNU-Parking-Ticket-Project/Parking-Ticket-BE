package com.jnu.ticketdomain.domains.registration.repository;


import com.jnu.ticketdomain.domains.registration.domain.Registration;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RegistrationRepositoryCustom {
    Boolean existsByEmailAndIsSavedTrueAndEvent(String email, Long eventId);

    Boolean existsByStudentNumAndIsSavedTrue(String studentNum, Long eventId);

    List<Registration> findSortedRegistrationsByEventId(Long eventId);
}
