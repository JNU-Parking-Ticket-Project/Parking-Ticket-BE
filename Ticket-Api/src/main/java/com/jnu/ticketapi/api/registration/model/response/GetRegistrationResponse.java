package com.jnu.ticketapi.api.registration.model.response;


import com.jnu.ticketapi.dto.SectorDto;
import java.util.List;
import lombok.Builder;

@Builder
public record GetRegistrationResponse(
        String email,
        String name,
        int studentNum,
        String affiliation,
        String carNum,
        boolean isLight,
        String phoneNum,
        List<SectorDto> sector,
        Long selectSectorId) {}
