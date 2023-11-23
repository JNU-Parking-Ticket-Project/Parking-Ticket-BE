package com.jnu.ticketapi.application.port;


import com.jnu.ticketapi.dto.TokenDto;
import com.jnu.ticketdomain.domain.dto.LoginUserRequestDto;
import com.jnu.ticketdomain.domain.user.User;
import java.util.Optional;

public interface UserUseCase {
    Optional<User> findByEmail(String email);

    User save(User user);

    TokenDto login(LoginUserRequestDto loginUserRequestDto);
}
