package com.jnu.ticketapi.api.auth.model.response;


import lombok.Builder;

@Builder
public record LoginUserResponse(String accessToken, String refreshToken) {}
