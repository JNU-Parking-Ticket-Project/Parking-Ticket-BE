package com.jnu.ticketapi.api.auth.model.response;

import lombok.Builder;

@Builder
public record LoginCouncilResponse(String accessToken, String refreshToken) {
}
