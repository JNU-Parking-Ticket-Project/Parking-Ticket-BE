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
import com.jnu.ticketcommon.consts.TicketStatic;
import com.jnu.ticketcommon.utils.Result;
import com.jnu.ticketdomain.domains.events.adaptor.EventAdaptor;
import com.jnu.ticketdomain.domains.events.adaptor.SectorAdaptor;
import com.jnu.ticketdomain.domains.events.domain.Event;
import com.jnu.ticketdomain.domains.events.domain.EventStatus;
import com.jnu.ticketdomain.domains.events.domain.Sector;
import com.jnu.ticketdomain.domains.events.exception.AlreadyCloseStatusException;
import com.jnu.ticketdomain.domains.events.exception.NotPublishEventException;
import com.jnu.ticketdomain.domains.registration.adaptor.RegistrationAdaptor;
import com.jnu.ticketdomain.domains.registration.domain.Registration;
import com.jnu.ticketdomain.domains.registration.exception.AlreadyExistRegistrationException;
import com.jnu.ticketdomain.domains.registration.exception.NotFoundRegistrationException;
import com.jnu.ticketdomain.domains.user.adaptor.UserAdaptor;
import com.jnu.ticketdomain.domains.user.domain.User;
import com.jnu.ticketinfrastructure.redis.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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
    private final ValidateCaptchaUseCase validateCaptchaUseCase;
    private final RedisService redisService;
    private final EventAdaptor eventAdaptor;

    public Registration saveAndFlush(Registration registration) {
        return registrationAdaptor.saveAndFlush(registration);
    }

    public Result<Registration, Object> findResultByEmail(
            String email, boolean flag, Long eventId) {
        Optional<Registration> registration =
                registrationAdaptor.findByEmailAndIsSaved(email, flag, eventId);
        return registration
                .filter(r -> r.getSector().getEvent().getId().equals(eventId))
                .map(Result::success)
                .orElseGet(() -> Result.failure(NotFoundRegistrationException.EXCEPTION));
    }

    public User findById(Long userId) {
        return userAdaptor.findById(userId);
    }

    @Transactional(readOnly = true)
    public GetRegistrationResponse getRegistration(String email, Long eventId) {
        Optional<Registration> registration =
                registrationAdaptor.findByEmailAndIsSaved(email, false, eventId);
        List<Sector> sectorList = sectorAdaptor.findAllByEventStatusAndPublishAndIsDeleted();
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
    public TemporarySaveResponse temporarySave(
            TemporarySaveRequest requestDto, String email, Long eventId) {
        Sector sector = sectorAdaptor.findById(requestDto.selectSectorId());
        Event event = eventAdaptor.findById(eventId);
        validateEventPublish(event);
        validateEventStatusIsClosed(event);
        Long currentUserId = SecurityUtils.getCurrentUserId();
        User user = findById(currentUserId);
        Registration registration = requestDto.toEntity(requestDto, sector, email, user);
        return findResultByEmail(email, false, eventId)
                .fold(
                        tempRegistration -> {
                            tempRegistration.update(registration);
                            return TemporarySaveResponse.from(tempRegistration);
                        },
                        emptyCase -> {
                            Registration jpaRegistration = saveAndFlush(registration);
                            return TemporarySaveResponse.from(jpaRegistration);
                        });
    }

    @Transactional
    public FinalSaveResponse finalSave(FinalSaveRequest requestDto, String email, Long eventId) {
        Sector sector = sectorAdaptor.findById(requestDto.selectSectorId());
        Event event = eventAdaptor.findById(eventId);
        validateEventPublish(event);
        validateEventStatusIsClosed(event);
        if (registrationAdaptor.existsByEmailAndIsSavedTrue(email, eventId)
                || registrationAdaptor.existsByStudentNumAndIsSavedTrue(
                        requestDto.studentNum(), eventId)) {
            throw AlreadyExistRegistrationException.EXCEPTION;
        }
        Long captchaId = encryption.decrypt(requestDto.captchaCode());
        validateCaptchaUseCase.execute(captchaId, requestDto.captchaAnswer());
        Long currentUserId = SecurityUtils.getCurrentUserId();
        User user = findById(currentUserId);

        Registration registration = requestDto.toEntity(requestDto, sector, email, user);
        return findResultByEmail(email, false, eventId)
                .fold(
                        tempRegistration ->
                                reFinalRegister(
                                        tempRegistration, registration, sector, user, email),
                        emptyCase -> saveRegistration(registration, sector, currentUserId, email));
    }

    private FinalSaveResponse reFinalRegister(
            Registration tempRegistration,
            Registration registration,
            Sector sector,
            User user,
            String email) {
        // 예비 번호가 있거나 합격인 경우
        sector.checkEventLeft();
        reFinalRegisterProcess(tempRegistration, registration, user, email, sector.getId());
        return FinalSaveResponse.from(tempRegistration);
    }

    private void reFinalRegisterProcess(
            Registration tempRegistration,
            Registration registration,
            User user,
            String email,
            Long sectorId) {
        tempRegistration.update(registration);
        eventWithDrawUseCase.issueEvent(registration, user.getId(), sectorId);
        redisService.deleteValues("RT(" + TicketStatic.SERVER + "):" + email);
    }

    private FinalSaveResponse saveRegistration(
            Registration registration, Sector sector, Long currentUserId, String email) {
        sector.checkEventLeft();
        return saveRegistrationProcess(registration, sector, currentUserId, email);
    }

    private FinalSaveResponse saveRegistrationProcess(
            Registration registration, Sector sector, Long currentUserId, String email) {
        //        Registration saveReg = saveAndFlush(registration);
        eventWithDrawUseCase.issueEvent(registration, currentUserId, sector.getId());
        redisService.deleteValues("RT(" + TicketStatic.SERVER + "):" + email);
        //        Events.raise(new EventIssuedEvent())
        return FinalSaveResponse.from(registration);
    }

    private void validateEventPublish(Event event) {
        if (Boolean.FALSE.equals(event.getPublish())) {
            throw NotPublishEventException.EXCEPTION;
        }
    }

    private void validateEventStatusIsClosed(Event event) {
        if (event.getEventStatus().equals(EventStatus.CLOSED) ||
                event.getDateTimePeriod().isAfterEndAt(LocalDateTime.now())) {
            throw AlreadyCloseStatusException.EXCEPTION;
        }
    }

    @Transactional(readOnly = true)
    public GetRegistrationsResponse getRegistrations(Long eventId) {
        List<Registration> registrations =
                registrationAdaptor.findByIsDeletedFalseAndIsSavedTrue(eventId);

        return GetRegistrationsResponse.of(registrations);
    }
}
