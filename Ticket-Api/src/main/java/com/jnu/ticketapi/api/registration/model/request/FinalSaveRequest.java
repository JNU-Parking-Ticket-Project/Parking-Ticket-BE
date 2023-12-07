package com.jnu.ticketapi.api.registration.model.request;


import java.util.Optional;
import lombok.Builder;

@Builder
public record FinalSaveRequest(
        String name,
        Integer studentNum,
        String affiliation,
        String carNum,
        boolean isLight,
        String phoneNum,
        Long selectSectorId,
        Optional<Long> registrationId) {}
