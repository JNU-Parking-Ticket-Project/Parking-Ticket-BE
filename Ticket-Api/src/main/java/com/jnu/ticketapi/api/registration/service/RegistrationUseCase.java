package com.jnu.ticketapi.api.registration.service;


import com.jnu.ticketapi.api.captcha.service.ValidateCaptchaUseCase;
import com.jnu.ticketapi.api.event.service.EventWithDrawUseCase;
import com.jnu.ticketapi.api.registration.model.request.FinalSaveRequest;
import com.jnu.ticketapi.api.registration.model.request.TemporarySaveRequest;
import com.jnu.ticketapi.api.registration.model.response.FinalSaveResponse;
import com.jnu.ticketapi.api.registration.model.response.GetRegistrationResponse;
import com.jnu.ticketapi.api.registration.model.response.GetRegistrationsResponse;
import com.jnu.ticketapi.api.registration.model.response.TemporarySaveResponse;
import com.jnu.ticketapi.application.helper.Converter;
import com.jnu.ticketapi.application.helper.Encryption;
import com.jnu.ticketapi.config.SecurityUtils;
import com.jnu.ticketcommon.annotation.UseCase;
import com.jnu.ticketcommon.utils.Result;
import com.jnu.ticketdomain.common.domainEvent.Events;
import com.jnu.ticketdomain.domains.captcha.adaptor.CaptchaAdaptor;
import com.jnu.ticketdomain.domains.events.adaptor.SectorAdaptor;
import com.jnu.ticketdomain.domains.events.domain.Sector;
import com.jnu.ticketdomain.domains.events.exception.NotFoundEventException;
import com.jnu.ticketdomain.domains.registration.adaptor.RegistrationAdaptor;
import com.jnu.ticketdomain.domains.registration.domain.Registration;
import com.jnu.ticketdomain.domains.registration.event.RegistrationCreationEvent;
import com.jnu.ticketdomain.domains.user.adaptor.UserAdaptor;
import com.jnu.ticketdomain.domains.user.domain.User;
import com.jnu.ticketinfrastructure.service.MailService;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class RegistrationUseCase {

    private final RegistrationAdaptor registrationAdaptor;
    private final SectorAdaptor sectorAdaptor;
    private final Converter converter;
    private final UserAdaptor userAdaptor;
    private final EventWithDrawUseCase eventWithDrawUseCase;
    private final Encryption encryption;
    private final CaptchaAdaptor captchaAdaptor;
    private final ValidateCaptchaUseCase validateCaptchaUseCase;
    private final MailService mailService;

    public Registration save(Registration registration) {
        return registrationAdaptor.save(registration);
    }

    public Optional<Registration> findByEmail(String email) {
        return registrationAdaptor.findByEmail(email);
    }

    public Result<Registration, Object> findResultByEmail(String email) {
        Optional<Registration> registration = registrationAdaptor.findByEmail(email);
        return registration
                .map(Result::success)
                .orElseGet(() -> Result.failure(NotFoundEventException.EXCEPTION));
    }

    public User findById(Long userId) {
        return userAdaptor.findById(userId);
    }

    @Transactional(readOnly = true)
    public GetRegistrationResponse getRegistration(String email) {
        Optional<Registration> registration = findByEmail(email);
        List<Sector> sectorList = sectorAdaptor.findAll();
        // 신청자가 임시저장을 하지 않았을 경우
        if (registration.isEmpty()) {
            return GetRegistrationResponse.builder()
                    .sectors(converter.toSectorDto(sectorList))
                    .email(email)
                    .build();
        }
        // 신청자가 임시저장을 했을 경우
        return converter.toGetRegistrationResponseDto(
                email, registration.get(), converter.toSectorDto(sectorList));
    }

    @Transactional
    public TemporarySaveResponse temporarySave(TemporarySaveRequest requestDto, String email) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        User user = findById(currentUserId);
        Sector sector = sectorAdaptor.findById(requestDto.selectSectorId());
        Registration registration = requestDto.toEntity(requestDto, sector, email, user);
        return findResultByEmail(email)
                .fold(
                        tempRegistration -> {
                            tempRegistration.update(registration);
                            return TemporarySaveResponse.from(tempRegistration);
                        },
                        emptyCase -> {
                            Registration jpaRegistration = save(registration);
                            return TemporarySaveResponse.from(jpaRegistration);
                        });
    }

    @Transactional
    public FinalSaveResponse finalSave(FinalSaveRequest requestDto, String email) {
        //        Long captchaId = encryption.decrypt(requestDto.captchaCode());
        //        validateCaptchaUseCase.execute(captchaId, requestDto.captchaAnswer());
        Long currentUserId = SecurityUtils.getCurrentUserId();
        User user = findById(currentUserId);

        Sector sector = sectorAdaptor.findById(requestDto.selectSectorId());
        Registration registration = requestDto.toEntity(requestDto, sector, email, user);
        return findResultByEmail(email)
                .fold(
                        tempRegistration ->
                                updateRegistration(tempRegistration, registration, currentUserId),
                        emptyCase -> saveRegistration(registration, currentUserId));
    }

    private FinalSaveResponse saveRegistration(Registration registration, Long currentUserId) {
        eventWithDrawUseCase.issueEvent(currentUserId);
        Events.raise(RegistrationCreationEvent.of(registration));
        return FinalSaveResponse.from(save(registration));
    }

    private FinalSaveResponse updateRegistration(
            Registration temporaryRegistration, Registration registration, Long currentUserId) {
        // update
        eventWithDrawUseCase.issueEvent(currentUserId);
        temporaryRegistration.update(registration);
        temporaryRegistration.updateIsSaved(true);
        Events.raise(RegistrationCreationEvent.of(registration));
        return FinalSaveResponse.from(temporaryRegistration);
    }

    @Transactional(readOnly = true)
    public GetRegistrationsResponse getRegistrations() {
        List<Registration> registrations = registrationAdaptor.findAll();
        return GetRegistrationsResponse.of(registrations);
    }
}
