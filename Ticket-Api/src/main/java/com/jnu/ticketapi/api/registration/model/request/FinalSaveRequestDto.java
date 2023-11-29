package com.jnu.ticketapi.api.registration.model.request;


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
