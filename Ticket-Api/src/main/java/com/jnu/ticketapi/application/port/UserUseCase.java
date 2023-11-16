package com.jnu.ticketapi.application.port;


import com.jnu.ticketdomain.domains.dto.LoginUserRpDto;
import com.jnu.ticketdomain.domains.dto.LoginUserRqDto;
import com.jnu.ticketdomain.domains.user.domian.User;
import java.util.Optional;

public interface UserUseCase {
    Optional<User> findByEmail(String email);

    User save(User user);

    LoginUserRpDto login(LoginUserRqDto loginUserRqDto);
}
