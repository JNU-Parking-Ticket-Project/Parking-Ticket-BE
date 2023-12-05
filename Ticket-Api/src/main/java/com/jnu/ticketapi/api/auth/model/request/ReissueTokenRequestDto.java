package com.jnu.ticketapi.api.auth.model.request;


import lombok.Builder;

@Builder
public record ReissueTokenRequestDto(String refreshToken) {}
