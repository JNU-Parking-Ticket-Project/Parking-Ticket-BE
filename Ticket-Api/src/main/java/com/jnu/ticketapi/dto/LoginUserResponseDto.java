package com.jnu.ticketapi.dto;


import lombok.Builder;

@Builder
public record LoginUserResponseDto(String accessToken, String refreshToken) {}
