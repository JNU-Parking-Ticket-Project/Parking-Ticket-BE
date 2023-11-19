package com.jnu.ticketapi.application.service;


import com.jnu.ticketapi.application.port.UserUseCase;
import com.jnu.ticketapi.common.errors.exception.Exception400;
import com.jnu.ticketdomain.domains.dto.LoginUserResponseDto;
import com.jnu.ticketdomain.domains.dto.LoginUserRequestsDto;
import com.jnu.ticketdomain.domains.user.adapter.UserAdaptor;
import com.jnu.ticketdomain.domains.user.domian.User;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService implements UserUseCase {
    private final UserAdaptor userAdaptor;

    @Override
    public Optional<User> findByEmail(String email) {
        return userAdaptor.findByEmail(email);
    }

    @Override
    public User save(User user) {
        return userAdaptor.save(user);
    }

    /*
    email로 유저를 찾아서 없으면 회원가입 후 토큰발급
    email로 유저를 찾아서 있으면 비밀번호가 맞는지 확인하고 맞으면 토큰발급
     */
    @Override
    public LoginUserResponseDto login(LoginUserRequestsDto loginUserRequestsDto) {
        BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
        User user = findByEmail(loginUserRequestsDto.email()).get();
        if (user == null) {
            save(loginUserRequestsDto.toEntity(loginUserRequestsDto));
            // TODO: 토큰 발급, 리프레쉬 쿠키에 구워주기
            return LoginUserResponseDto.builder().accessToken(null).build();
        }
        if (!bCryptPasswordEncoder.matches(loginUserRequestsDto.pwd(), user.getPwd())) {
            throw new Exception400("비밀번호가 틀렸습니다.");
        }
        return LoginUserResponseDto.builder().accessToken(null).build();
    }
}
