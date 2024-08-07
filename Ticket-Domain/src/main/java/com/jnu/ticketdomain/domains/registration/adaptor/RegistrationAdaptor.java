package com.jnu.ticketdomain.domains.registration.adaptor;


import com.jnu.ticketcommon.annotation.Adaptor;
import com.jnu.ticketdomain.domains.registration.domain.Registration;
import com.jnu.ticketdomain.domains.registration.exception.NotFoundRegistrationException;
import com.jnu.ticketdomain.domains.registration.out.RegistrationLoadPort;
import com.jnu.ticketdomain.domains.registration.out.RegistrationRecordPort;
import com.jnu.ticketdomain.domains.registration.repository.RegistrationRepository;
import com.jnu.ticketdomain.domains.user.domain.User;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Adaptor
public class RegistrationAdaptor implements RegistrationLoadPort, RegistrationRecordPort {
    private final RegistrationRepository registrationRepository;

    @Override
    public Registration findByUserIdAndEventId(Long userId, Long eventId) {
        return registrationRepository.findByUserIdAndEventId(userId, eventId).orElse(null);
    }

    @Override
    public Registration saveAndFlush(Registration registration) {
        return registrationRepository.saveAndFlush(registration);
    }

    @Override
    public void delete(Registration registration) {
        registrationRepository.delete(registration);
    }

    @Override
    public void deleteBySector(Long sectorId) {
        registrationRepository.deleteBySectorId(sectorId);
    }

    @Override
    public void deleteByEvent(Long eventId) {
        registrationRepository.deleteByEventId(eventId);
    }

    @Override
    public Registration findById(Long id) {
        return registrationRepository
                .findById(id)
                .orElseThrow(() -> NotFoundRegistrationException.EXCEPTION);
    }

    @Override
    public List<Registration> findAll() {
        return registrationRepository.findAll();
    }

    @Override
    public Optional<Registration> findByEmail(String email) {
        return registrationRepository.findByEmail(email);
    }

    @Override
    public Optional<Registration> findByEmailAndIsSaved(String email, boolean flag, Long eventId) {
        return registrationRepository.findByEmailAndIsSaved(email, flag);
    }

    @Override
    public List<Registration> findByIsDeletedFalseAndIsSavedTrue(Long eventId) {
        return registrationRepository.findByIsDeletedFalseAndIsSavedTrue(eventId);
    }

    @Override
    public Boolean existsByEmailAndIsSavedTrue(String email, Long eventId) {
        return registrationRepository.existsByEmailAndIsSavedTrueAndEvent(email, eventId);
    }

    @Override
    public Boolean existsByStudentNumAndIsSavedTrue(String studentNum, Long eventId) {
        return registrationRepository.existsByStudentNumAndIsSavedTrue(studentNum, eventId);
    }

    public List<Registration> findByUser(User user) {
        return registrationRepository.findByUser(user);
    }

    public List<Registration> findByUserId(Long userId) {
        return registrationRepository.findByUserId(userId);
    }
}
