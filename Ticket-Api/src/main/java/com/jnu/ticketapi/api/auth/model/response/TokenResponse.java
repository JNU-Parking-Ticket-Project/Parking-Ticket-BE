package com.jnu.ticketapi.api.auth.model.response;


import lombok.Builder;

@Builder
public record TokenResponse(String accessToken, String refreshToken) {}
