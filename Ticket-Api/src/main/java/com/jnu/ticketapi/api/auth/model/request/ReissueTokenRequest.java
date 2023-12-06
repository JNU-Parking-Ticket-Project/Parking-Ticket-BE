package com.jnu.ticketapi.api.auth.model.request;


import lombok.Builder;

@Builder
public record ReissueTokenRequest(String refreshToken) {}
