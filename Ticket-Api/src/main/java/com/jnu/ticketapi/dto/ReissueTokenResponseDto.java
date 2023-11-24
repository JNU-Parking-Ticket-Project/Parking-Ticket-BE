package com.jnu.ticketapi.dto;


import lombok.Builder;

@Builder
public record ReissueTokenResponseDto(String accessToken, String refreshToken) {}
