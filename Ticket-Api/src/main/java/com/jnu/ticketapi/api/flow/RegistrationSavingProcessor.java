package com.jnu.ticketapi.api.flow;

import com.jnu.ticketdomain.domains.events.adaptor.SectorAdaptor;
import com.jnu.ticketdomain.domains.events.domain.Sector;
import com.jnu.ticketdomain.domains.registration.adaptor.RegistrationAdaptor;
import com.jnu.ticketdomain.domains.registration.domain.Registration;
import com.jnu.ticketdomain.domains.user.adaptor.UserAdaptor;
import com.jnu.ticketdomain.domains.user.domain.User;
import com.jnu.ticketinfrastructure.model.RegistrationInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class RegistrationSavingProcessor {

    private final SectorAdaptor sectorAdaptor;
    private final UserAdaptor userAdaptor;
    private final RegistrationAdaptor registrationAdaptor;

    @Retryable(
            retryFor = {Exception.class},
            noRetryFor = {DataIntegrityViolationException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 500)
    )

    @Transactional
    public void process(RegistrationInfo registrationInfo) {
        Long sectorId = registrationInfo.getSectorId();
        Sector sector = sectorAdaptor.findById(sectorId);
        User user = userAdaptor.findById(registrationInfo.getUserId());

        Registration registration = createRegistration(registrationInfo, sector, user);
        registration.finalSave();

        registrationAdaptor.save(registration);
        registrationAdaptor.updateSavedAt(registration);
    }

    private Registration createRegistration(RegistrationInfo registrationInfo, Sector sector, User user) {
        Registration registration = Registration.builder()
                .eventId(registrationInfo.getEventId())
                .email(registrationInfo.getEmail())
                .name(registrationInfo.getName())
                .studentNum(registrationInfo.getStudentNum())
                .affiliation(registrationInfo.getAffiliation())
                .department(registrationInfo.getDepartment())
                .carNum(registrationInfo.getCarNum())
                .phoneNum(registrationInfo.getPhoneNum())
                .isLight(registrationInfo.isLight())
                .isSaved(registrationInfo.isSaved())
                .savedAt(registrationInfo.getSavedAt())
                .build();
        String createdAt = registrationInfo.getCreatedAt();
        if (createdAt != null) {
            registration.setCreatedAt(LocalDateTime.parse(createdAt));
        }
        registration.setId(registrationInfo.getId());
        registration.setSector(sector);
        registration.setUser(user);
        return registration;
    }

}

