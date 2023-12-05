package com.jnu.ticketapi.api.registration.service;


import com.jnu.ticketapi.api.coupon.service.CouponWithDrawUseCase;
import com.jnu.ticketapi.api.registration.model.request.FinalSaveRequest;
import com.jnu.ticketapi.api.registration.model.request.TemporarySaveRequest;
import com.jnu.ticketapi.api.registration.model.response.FinalSaveResponse;
import com.jnu.ticketapi.api.registration.model.response.GetRegistrationResponse;
import com.jnu.ticketapi.api.registration.model.response.TemporarySaveResponse;
import com.jnu.ticketapi.application.helper.Converter;
import com.jnu.ticketapi.config.SecurityUtils;
import com.jnu.ticketcommon.annotation.UseCase;
import com.jnu.ticketcommon.message.ResponseMessage;
import com.jnu.ticketdomain.domains.coupon.adaptor.SectorAdaptor;
import com.jnu.ticketdomain.domains.coupon.domain.Sector;
import com.jnu.ticketdomain.domains.registration.adaptor.RegistrationAdaptor;
import com.jnu.ticketdomain.domains.registration.domain.Registration;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
@UseCase
@RequiredArgsConstructor
public class RegistrationUseCase {
    private final RegistrationAdaptor registrationAdaptor;
    private final SectorAdaptor sectorAdaptor;
    private final Converter converter;
    private final CouponWithDrawUseCase couponWithDrawUseCase;

    public Registration findByUserId(Long userId) {
        return registrationAdaptor.findByUserId(userId);
    }

    public Registration save(Registration registration) {
        return registrationAdaptor.save(registration);
    }

    @Transactional(readOnly = true)
    public GetRegistrationResponse getRegistration(String email) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        Registration registration = findByUserId(currentUserId);
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

    @Transactional
    public TemporarySaveResponse temporarySave(TemporarySaveRequest requestDto, String email) {
        Sector sector = sectorAdaptor.findById(requestDto.selectSectorId());
        Registration registration = converter.temporaryToRegistration(requestDto, sector, email);
        Registration jpaRegistration = save(registration);
        return converter.toTemporarySaveResponseDto(jpaRegistration);
    }

    @Transactional
    public FinalSaveResponse finalSave(FinalSaveRequest requestDto, String email) {
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
        Registration registration = converter.finalToRegistration(requestDto, sector, email);
        Registration jpaRegistration = save(registration);
        couponWithDrawUseCase.issueCoupon();
        return converter.toFinalSaveResponseDto(jpaRegistration);
    }
}
