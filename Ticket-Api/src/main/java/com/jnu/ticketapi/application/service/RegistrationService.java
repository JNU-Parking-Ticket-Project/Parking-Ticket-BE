package com.jnu.ticketapi.application.service;


import com.jnu.ticketapi.application.helper.Converter;
import com.jnu.ticketapi.application.port.RegistrationUseCase;
import com.jnu.ticketapi.dto.*;
import com.jnu.ticketdomain.domains.coupon.adaptor.SectorAdaptor;
import com.jnu.ticketdomain.domains.coupon.domain.Sector;
import com.jnu.ticketdomain.domains.registration.adaptor.RegistrationAdaptor;
import com.jnu.ticketdomain.domains.registration.domain.Registration;
import com.jnu.ticketdomain.domains.user.adaptor.UserAdaptor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RegistrationService implements RegistrationUseCase {
    private final RegistrationAdaptor registrationAdaptor;
    private final SectorAdaptor sectorAdaptor;
    private final UserAdaptor userAdaptor;
    private final Converter converter;

    @Override
    public Registration findByUserId(Long userId) {
        return registrationAdaptor.findByUserId(userId);
    }


    @Override
    @Transactional(readOnly = true )
    public GetRegistrationResponseDto getRegistration(Long userId, String email) {
        Registration registration = findByUserId(userId);
        List<Sector> sectorList = sectorAdaptor.findAll();
        // 신청자가 임시저장을 하지 않았을 경우
        if (registration == null) {
            return GetRegistrationResponseDto.builder()
                    .sector(converter.sectorToDto(sectorList))
                    .email(email)
                    .build();
        }
        // 신청자가 임시저장을 했을 경우
        return converter.toGetRegistrationResponseDto(email, registration, converter.sectorToDto(sectorList));
    }
}
