package com.jnu.ticketapi.api.registration.model.request;


import lombok.Builder;

@Builder
public record TemporarySaveRequest(
        String name,
        String studentNum,
        String affiliation,
        String carNum,
        boolean isLight,
        String phoneNum,
        Long selectSectorId) {}
