package com.jnu.ticketdomain.domains.registration.repository;


import org.springframework.stereotype.Repository;

@Repository
public interface RegistrationRepositoryCustom {
    Boolean existsByEmailAndIsSavedTrueAndEvent(String email, Long eventId);

    Boolean existsByStudentNumAndIsSavedTrue(String studentNum, Long eventId);
}
