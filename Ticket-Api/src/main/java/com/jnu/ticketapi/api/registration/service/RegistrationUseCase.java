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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
    private final ValidateCaptchaUseCase validateCaptchaUseCase;

    @Autowired(required = false)
    private RedisService redisService;

    private final EventAdaptor eventAdaptor;

    @Value("${ableRedis:true}")
    private boolean ableRedis;

    public Registration saveAndFlush(Registration registration) {
        return registrationAdaptor.saveAndFlush(registration);
    }

    public Result<Registration, Object> findResultByEmail(
            String email, boolean flag, Long eventId) {
        Optional<Registration> registration =
                registrationAdaptor.findByEmailAndIsSaved(email, flag).stream().findFirst();
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
                registrationAdaptor.findByEmailAndIsSaved(email, false).stream().findFirst();
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
        Sector sector = sectorAdaptor.findById(requestDto.selectSectorId()); // 구간
        Event event = eventAdaptor.findById(eventId);
        validateEventPublish(event);
        validateEventStatusIsClosed(event);
        Long currentUserId = SecurityUtils.getCurrentUserId();
        User user = findById(currentUserId);
        Registration registration =
                requestDto.toEntity(requestDto, sector, email, user, event.getId()); // 등록을 만듦
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
        validateEventPeriod(event);

        validateCaptchaUseCase.execute(requestDto.captchaCode(), requestDto.captchaAnswer());
        checkDuplicateRegistration(email, eventId);
        Long currentUserId = SecurityUtils.getCurrentUserId();
        User user = findById(currentUserId);

        Registration registration =
                requestDto.toEntity(requestDto, sector, email, user, event.getId());
        return findResultByEmail(email, false, eventId)
                .fold(
                        tempRegistration ->
                                reFinalRegister(
                                        tempRegistration,
                                        registration,
                                        sector,
                                        user,
                                        email,
                                        eventId),
                        emptyCase ->
                                saveRegistration(
                                        registration, sector, currentUserId, email, eventId));
    }

    private FinalSaveResponse reFinalRegister(
            Registration tempRegistration,
            Registration registration,
            Sector sector,
            User user,
            String email,
            Long eventId) {
        sector.checkEventLeft();
        registration.setId(tempRegistration.getId()); // update 치기 위해 Id 값을 채워줌
        registration.setCreatedAt(
                tempRegistration.getCreatedAt()); // create_at이 null이 들어가지 않게 하기 위해 값을 채워줌
        reFinalRegisterProcess(registration, user, email, sector.getId(), eventId);
        return FinalSaveResponse.from(registration);
    }

    private void reFinalRegisterProcess(
            Registration registration, User user, String email, Long sectorId, Long eventId) {
        eventWithDrawUseCase.issueEvent(registration, user.getId(), sectorId, eventId);
        if (ableRedis) {
            redisService.deleteValues("RT(" + TicketStatic.SERVER + "):" + email);
        }
    }

    private FinalSaveResponse saveRegistration(
            Registration registration,
            Sector sector,
            Long currentUserId,
            String email,
            Long eventId) {
        sector.checkEventLeft();
        return saveRegistrationProcess(registration, sector, currentUserId, email, eventId);
    }

    private FinalSaveResponse saveRegistrationProcess(
            Registration registration,
            Sector sector,
            Long currentUserId,
            String email,
            Long eventId) {
        //        Registration saveReg = saveAndFlush(registration);
        eventWithDrawUseCase.issueEvent(registration, currentUserId, sector.getId(), eventId);
        if (ableRedis) {
            redisService.deleteValues("RT(" + TicketStatic.SERVER + "):" + email);
        }
        //        Events.raise(new EventIssuedEvent())
        return FinalSaveResponse.from(registration);
    }

    private void validateEventPublish(Event event) {
        if (Boolean.FALSE.equals(event.getPublish())) {
            throw NotPublishEventException.EXCEPTION;
        }
    }

    private void validateEventStatusIsClosed(Event event) {
        if (event.getEventStatus().equals(EventStatus.CLOSED)) {
            throw AlreadyCloseStatusException.EXCEPTION;
        }
    }

    private void validateEventPeriod(Event event) {
        if (event.getDateTimePeriod().isAfterEndAt(LocalDateTime.now())
                && event.getDateTimePeriod().isBeforeStartAt(LocalDateTime.now())) {
            throw AlreadyCloseStatusException.EXCEPTION;
        }
    }

    /*
     * 신청자 목록 조회
     * 먼저 구간 순으로 정렬하고, 합격자가 예비자보다 우선되도록 정렬, 합격자는 id로 정렬, 예비자는 sequence로 정렬
     */
    @Transactional(readOnly = true)
    public GetRegistrationsResponse getRegistrations(Long eventId) {
        List<Registration> registrations =
                registrationAdaptor.findSortedRegistrationsByEventId(eventId);
        return GetRegistrationsResponse.of(registrations);
    }

    private void checkDuplicateRegistration(String email, Long eventId) {
        if (registrationAdaptor.existsByEmailAndIsSavedTrue(email, eventId)) {
            throw AlreadyExistRegistrationException.EXCEPTION;
        }
    }

    private Integer parseSectorNumber(String sectorNumber) {
        return Integer.parseInt(sectorNumber.split("구간")[0]);
    }
}
