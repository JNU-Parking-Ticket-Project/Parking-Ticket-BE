package com.jnu.ticketapi.api.auth.model.response;


import lombok.Builder;

@Builder
public record ReissueTokenResponse(String accessToken, String refreshToken) {}
