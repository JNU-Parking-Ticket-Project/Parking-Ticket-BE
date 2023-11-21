package com.jnu.ticketdomain.domain.dto;


import lombok.Builder;

@Builder
public record LoginUserResponseDto(String accessToken) {}
