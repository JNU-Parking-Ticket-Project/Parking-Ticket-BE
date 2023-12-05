package com.jnu.ticketapi.api.auth.model.response;


import lombok.Builder;

@Builder
public record ReissueTokenResponseDto(String accessToken, String refreshToken) {}
