package com.jnu.ticketapi.application.port;


import com.jnu.ticketdomain.domains.dto.LoginUserResponseDto;
import com.jnu.ticketdomain.domains.dto.LoginUserRequestsDto;
import com.jnu.ticketdomain.domains.user.domian.User;
import java.util.Optional;

public interface UserUseCase {
    Optional<User> findByEmail(String email);

    User save(User user);

    LoginUserResponseDto login(LoginUserRequestsDto loginUserRequestsDto);
}
