package com.jnu.ticketapi.api.registration.model.response;


import lombok.Builder;

@Builder
public record TemporarySaveResponse(Long registrationId, String message) {}
