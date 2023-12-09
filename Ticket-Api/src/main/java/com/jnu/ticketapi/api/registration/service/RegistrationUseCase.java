package com.jnu.ticketapi.api.registration.service;


import com.jnu.ticketapi.api.captcha.service.ValidateCaptchaPendingUseCase;
import com.jnu.ticketapi.api.coupon.service.CouponWithDrawUseCase;
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
import com.jnu.ticketdomain.domains.captcha.adaptor.CaptchaAdaptor;
import com.jnu.ticketdomain.domains.coupon.adaptor.SectorAdaptor;
import com.jnu.ticketdomain.domains.coupon.domain.Sector;
import com.jnu.ticketdomain.domains.registration.adaptor.RegistrationAdaptor;
import com.jnu.ticketdomain.domains.registration.domain.Registration;
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
    private final CouponWithDrawUseCase couponWithDrawUseCase;
    private final Encryption encryption;
    private final CaptchaAdaptor captchaAdaptor;
    private final ValidateCaptchaPendingUseCase validateCaptchaPendingUseCase;
    private final MailService mailService;

    public Registration save(Registration registration) {
        return registrationAdaptor.save(registration);
    }

    public Optional<Registration> findByEmail(String email) {
        return registrationAdaptor.findByEmail(email);
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
        Optional<Registration> temporaryRegistration = findByEmail(email);
        if (temporaryRegistration.isPresent()) {
            temporaryRegistration.get().update(registration);
            return TemporarySaveResponse.from(temporaryRegistration.get());
        }
        Registration jpaRegistration = save(registration);
        return TemporarySaveResponse.from(jpaRegistration);
    }

    @Transactional
    public FinalSaveResponse finalSave(FinalSaveRequest requestDto, String email) {
        Long captchaPendingId = encryption.decrypt(requestDto.captchaPendingCode());
        validateCaptchaPendingUseCase.execute(captchaPendingId, requestDto.captchaAnswer());
        /*
        임시저장을 했으면 isSave만 true로 변경
         */
        Long currentUserId = SecurityUtils.getCurrentUserId();
        User user = findById(currentUserId);
        Sector sector = sectorAdaptor.findById(requestDto.selectSectorId());
        Registration registration = requestDto.toEntity(requestDto, sector, email, user);
        Optional<Registration> temporaryRegistration = findByEmail(email);
        if (temporaryRegistration.isPresent()) {
            temporaryRegistration.get().update(registration);
            temporaryRegistration.get().updateIsSaved(true);
            mailService.sendRegistrationResultMail(
                    registration.getEmail(),
                    registration.getName(),
                    registration.getUser().getStatus());
            return FinalSaveResponse.from(temporaryRegistration.get());
        }
        Registration jpaRegistration = save(registration);
        couponWithDrawUseCase.issueCoupon();

        mailService.sendRegistrationResultMail(
                registration.getEmail(),
                registration.getName(),
                registration.getUser().getStatus());

        return FinalSaveResponse.from(jpaRegistration);
    }

    @Transactional(readOnly = true)
    public GetRegistrationsResponse getRegistrations() {
        List<Registration> registrations = registrationAdaptor.findAll();
        return GetRegistrationsResponse.of(registrations);
    }
}
