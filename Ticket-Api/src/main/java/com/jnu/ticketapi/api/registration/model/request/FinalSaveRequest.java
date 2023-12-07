package com.jnu.ticketapi.api.registration.model.request;


import lombok.Builder;

import java.util.Optional;

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
