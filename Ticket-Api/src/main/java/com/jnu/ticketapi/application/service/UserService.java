package com.jnu.ticketapi.application.service;


import com.jnu.ticketapi.application.port.UserUseCase;
import com.jnu.ticketapi.common.errors.exception.Exception400;
import com.jnu.ticketdomain.domains.dto.LoginUserRpDto;
import com.jnu.ticketdomain.domains.dto.LoginUserRqDto;
import com.jnu.ticketdomain.domains.user.adapter.UserAdapter;
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
    private final UserAdapter userAdapter;

    @Override
    public Optional<User> findByEmail(String email) {
        return userAdapter.findByEmail(email);
    }

    @Override
    public User save(User user) {
        return userAdapter.save(user);
    }

    /*
    email로 유저를 찾아서 없으면 회원가입 후 토큰발급
    email로 유저를 찾아서 있으면 비밀번호가 맞는지 확인하고 맞으면 토큰발급
     */
    @Override
    public LoginUserRpDto login(LoginUserRqDto loginUserRqDto) {
        BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
        User user = findByEmail(loginUserRqDto.email()).get();
        if (user == null) {
            save(loginUserRqDto.toEntity(loginUserRqDto));
            // TODO: 토큰 발급, 리프레쉬 쿠키에 구워주기
            return LoginUserRpDto.builder().accessToken(null).build();
        }
        if (!bCryptPasswordEncoder.matches(loginUserRqDto.pwd(), user.getPwd())) {
            throw new Exception400("비밀번호가 틀렸습니다.");
        }
        return LoginUserRpDto.builder().accessToken(null).build();
    }
}
