package com.jnu.ticketapi.dto;


import lombok.Builder;

@Builder
public record FinalSaveRequestDto (
        String email,
        String name,
        int studentNum,
        String affiliation,
        String carNum,
        boolean isLight,
        String phoneNum,
        Long selectSectorId,
        Long registrationId) {}
