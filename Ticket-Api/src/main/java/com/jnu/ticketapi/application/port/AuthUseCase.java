package com.jnu.ticketapi.application.port;


import com.jnu.ticketapi.dto.LoginUserRequestDto;
import com.jnu.ticketapi.dto.LoginUserResponseDto;
import com.jnu.ticketapi.dto.ReissueTokenResponseDto;
import com.jnu.ticketapi.dto.TokenDto;

public interface AuthUseCase {
    boolean validate(String refreshToken);

    ReissueTokenResponseDto reissue(String requestAccessTokenInHeader, String requestRefreshToken);

    TokenDto generateToken(String provider, String email, String authorities);

    void saveRefreshToken(String provider, String principal, String refreshToken);

    LoginUserResponseDto login(LoginUserRequestDto loginUserRequestDto);

    void logout(String requestAccessTokenInHeader);

    String extractToken(String bearerToken);
}
