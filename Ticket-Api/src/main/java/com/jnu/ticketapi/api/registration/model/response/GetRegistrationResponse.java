package com.jnu.ticketapi.api.registration.model.response;


import com.jnu.ticketapi.api.sector.model.internal.SectorDto;
import java.util.List;
import lombok.Builder;

@Builder
public record GetRegistrationResponse(
        String email,
        String name,
        String studentNum,
        String affiliation,
        String carNum,
        boolean isLight,
        String phoneNum,
        List<SectorDto> sectors,
        Long selectSectorId) {}
