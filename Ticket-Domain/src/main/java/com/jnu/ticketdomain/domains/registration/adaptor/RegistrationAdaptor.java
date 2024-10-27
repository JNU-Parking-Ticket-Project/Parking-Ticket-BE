package com.jnu.ticketdomain.domains.registration.adaptor;

import static com.jnu.ticketcommon.consts.TicketStatic.REGISTRATION_SIZE;

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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

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
    public Registration save(Registration registration) {
        return registrationRepository.save(registration);
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
    public List<Registration> findByEmailAndIsSaved(String email, boolean flag) {
        return registrationRepository.findByEmailAndIsSaved(email, flag);
    }

    @Override
    public List<Registration> findByIsDeletedFalseAndIsSavedTrue(Long eventId) {
        return registrationRepository.findByIsDeletedFalseAndIsSavedTrue(eventId);
    }

    public Page<Registration> findByIsDeletedFalseAndIsSavedTrueByPage(Long eventId, int page) {
        Pageable pageable = PageRequest.of(page, REGISTRATION_SIZE);
        return registrationRepository.findByIsDeletedFalseAndIsSavedTrueByPage(eventId, pageable);
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

    @Override
    public Integer findPositionBySavedAt(Long id, Long sectorId) {
        return registrationRepository.findPositionBySavedAt(id, sectorId) + 1;
    }

    @Override
    public Boolean existsByIdAndIsSavedTrue(Long id) {
        return registrationRepository.existsByIdAndIsSavedTrue(id);
    }
}
