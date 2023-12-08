package com.jnu.ticketapi.api.auth.model.internal;


import lombok.Builder;

@Builder
public record TokenDto(String accessToken, String refreshToken) {}
