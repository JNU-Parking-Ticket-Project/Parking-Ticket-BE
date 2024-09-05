package com.jnu.ticketdomain.domains.registration.out;


import com.jnu.ticketcommon.annotation.Port;
import com.jnu.ticketdomain.domains.registration.domain.Registration;
import java.util.List;
import java.util.Optional;

@Port
public interface RegistrationLoadPort {
    Registration findByUserIdAndEventId(Long userId, Long eventId);

    Registration findById(Long id);

    List<Registration> findAll();

    Optional<Registration> findByEmail(String email);

    List<Registration> findByIsDeletedFalseAndIsSavedTrue(Long eventId);

    Boolean existsByEmailAndIsSavedTrue(String email, Long eventId);

    Boolean existsByStudentNumAndIsSavedTrue(String studentNum, Long eventId);

    Optional<Registration> findByEmailAndIsSaved(String email, boolean flag);

    Integer findPositionById(Long id, Long sectorId);

    Boolean existsByIdAndIsSavedTrue(Long id);
}
