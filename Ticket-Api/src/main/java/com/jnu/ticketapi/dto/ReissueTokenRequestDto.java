package com.jnu.ticketapi.dto;


import lombok.Builder;

@Builder
public record ReissueTokenRequestDto(String refreshToken) {}