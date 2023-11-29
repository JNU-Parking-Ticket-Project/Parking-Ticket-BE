package com.jnu.ticketapi.dto;


import lombok.Builder;

@Builder
public record FinalSaveResponseDto(Long registrationId, String message) {}
