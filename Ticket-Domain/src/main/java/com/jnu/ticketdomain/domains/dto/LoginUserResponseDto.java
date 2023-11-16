package com.jnu.ticketdomain.domains.dto;


import lombok.Builder;

@Builder
public record LoginUserResponseDto(String accessToken) {}
