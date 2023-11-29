package com.jnu.ticketapi.dto;


import java.util.List;
import lombok.Builder;

@Builder
public record GetRegistrationResponseDto(
        String email,
        String name,
        int studentNum,
        String affiliation,
        String carNum,
        boolean isLight,
        String phoneNum,
        List<SectorDto> sector,
        Long selectSectorId) {}
