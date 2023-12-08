package com.jnu.ticketapi.api.registration.service;


import com.jnu.ticketapi.api.coupon.service.CouponWithDrawUseCase;
import com.jnu.ticketapi.api.registration.model.request.FinalSaveRequest;
import com.jnu.ticketapi.api.registration.model.request.TemporarySaveRequest;
import com.jnu.ticketapi.api.registration.model.response.FinalSaveResponse;
import com.jnu.ticketapi.api.registration.model.response.GetRegistrationResponse;
import com.jnu.ticketapi.api.registration.model.response.TemporarySaveResponse;
import com.jnu.ticketapi.application.helper.Converter;
import com.jnu.ticketapi.application.port.RegistrationUseCase;
import com.jnu.ticketapi.config.SecurityUtils;
import com.jnu.ticketcommon.message.ResponseMessage;
import com.jnu.ticketdomain.domains.coupon.adaptor.SectorAdaptor;
import com.jnu.ticketdomain.domains.coupon.domain.Sector;
import com.jnu.ticketdomain.domains.registration.adaptor.RegistrationAdaptor;
import com.jnu.ticketdomain.domains.registration.domain.Registration;
import com.jnu.ticketdomain.domains.user.adaptor.UserAdaptor;
import com.jnu.ticketdomain.domains.user.domain.User;
import com.jnu.ticketdomain.domains.user.exception.NotFoundUserException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RegistrationService implements RegistrationUseCase {
    private final RegistrationAdaptor registrationAdaptor;
    private final SectorAdaptor sectorAdaptor;
    private final UserAdaptor userAdaptor;
    private final Converter converter;
    private final CouponWithDrawUseCase couponWithDrawUseCase;

    @Override
    public Registration findByUserId(Long userId) {
        return registrationAdaptor.findByUserId(userId);
    }

    @Override
    public Registration save(Registration registration) {
        return registrationAdaptor.save(registration);
    }

    @Override
    @Transactional(readOnly = true)
    public GetRegistrationResponse getRegistration(String email) {
        User user =
                userAdaptor.findByEmail(email).orElseThrow(() -> NotFoundUserException.EXCEPTION);
        Long userId = user.getId();
        Registration registration = findByUserId(userId);
        List<Sector> sectorList = sectorAdaptor.findAll();
        // 신청자가 임시저장을 하지 않았을 경우
        if (registration == null) {
            return GetRegistrationResponse.builder()
                    .sector(converter.toSectorDto(sectorList))
                    .email(email)
                    .build();
        }
        // 신청자가 임시저장을 했을 경우
        return converter.toGetRegistrationResponseDto(
                email, registration, converter.toSectorDto(sectorList));
    }

    @Override
    @Transactional
    public TemporarySaveResponse temporarySave(TemporarySaveRequest requestDto, String email) {
        Sector sector = sectorAdaptor.findById(requestDto.selectSectorId());
        Registration registration = converter.temporaryToRegistration(requestDto, sector, email);
        Registration jpaRegistration = save(registration);
        return converter.toTemporarySaveResponseDto(jpaRegistration);
    }

    @Override
    @Transactional
    public FinalSaveResponse finalSave(FinalSaveRequest requestDto, String email) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        User user = userAdaptor.findById(currentUserId).get();
        /*
        임시저장을 했으면 isSave만 true로 변경
         */
        if (requestDto.registrationId() != null) {
            Registration registration = registrationAdaptor.findById(requestDto.registrationId());
            registration.updateIsSaved(true);
            return FinalSaveResponse.builder()
                    .registrationId(registration.getId())
                    .message(ResponseMessage.SUCCESS_FINAL_SAVE)
                    .build();
        }
        Sector sector = sectorAdaptor.findById(requestDto.selectSectorId());
        Registration registration = converter.finalToRegistration(requestDto, sector, email,user);
        Registration jpaRegistration = save(registration);
        couponWithDrawUseCase.issueCoupon(currentUserId);
        return converter.toFinalSaveResponseDto(jpaRegistration);
    }
}
