package com.jnu.ticketapi.application.port;


import com.jnu.ticketapi.dto.TokenDto;

public interface AuthUseCase {
    boolean validate(String requestAccessTokenInHeader);

    TokenDto reissue(String requestAccessTokenInHeader, String requestRefreshToken);

    TokenDto generateToken(String provider, String email, String authorities);

    void saveRefreshToken(String provider, String principal, String refreshToken);

    String resolveToken(String requestAccessTokenInHeader);

    void logout(String requestAccessTokenInHeader);
}
