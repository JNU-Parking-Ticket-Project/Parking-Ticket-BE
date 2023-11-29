package com.jnu.ticketapi.api.registration.model.response;


import lombok.Builder;

@Builder
public record FinalSaveResponseDto(Long registrationId, String message) {}
