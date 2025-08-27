package com.jnu.ticketapi.api.flow;

import com.jnu.ticketdomain.domains.events.adaptor.SectorAdaptor;
import com.jnu.ticketdomain.domains.events.domain.Sector;
import com.jnu.ticketdomain.domains.registration.adaptor.RegistrationAdaptor;
import com.jnu.ticketdomain.domains.registration.domain.Registration;
import com.jnu.ticketdomain.domains.user.adaptor.UserAdaptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

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
    public void process(Map<String, String> RegistrationDto) {
        long sectorId = Long.parseLong(RegistrationDto.get("sectorId"));
        Sector sector = sectorAdaptor.findById(sectorId);
        Registration registration = Registration.builder()
                .eventId(Long.valueOf(RegistrationDto.get("eventId")))
                .email(RegistrationDto.get("email"))
                .name(RegistrationDto.get("name"))
                .studentNum(RegistrationDto.get("studentNum"))
                .affiliation(RegistrationDto.get("affiliation"))
                .department(RegistrationDto.get("department"))
                .carNum(RegistrationDto.get("carNum"))
                .phoneNum(RegistrationDto.get("phoneNum"))
                .isLight(Boolean.parseBoolean(RegistrationDto.get("isLight")))
                .isSaved(Boolean.parseBoolean(RegistrationDto.get("isSaved")))
                .savedAt(Long.valueOf(RegistrationDto.get("savedAt")))
                .build();
        String registrationId = RegistrationDto.get("id");
        registration.setSector(sector);

        String createAt = RegistrationDto.get("createAt");
        if (createAt != null) {
            registration.setCreatedAt(LocalDateTime.parse(createAt));
        }
        if (registrationId != null) {
            registration.setId(Long.valueOf(registrationId));
        }
        registration.setUser(userAdaptor.findById(Long.valueOf(RegistrationDto.get("userId"))));
        registration.finalSave();

        registrationAdaptor.save(registration);
        registrationAdaptor.updateSavedAt(registration);
    }

}

