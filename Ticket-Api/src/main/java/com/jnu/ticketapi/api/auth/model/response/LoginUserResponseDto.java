package com.jnu.ticketapi.api.auth.model.response;


import lombok.Builder;

@Builder
public record LoginUserResponseDto(String accessToken, String refreshToken) {}
