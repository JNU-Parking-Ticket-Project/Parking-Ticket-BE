package com.jnu.ticketapi.application.service;


import com.jnu.ticketapi.application.port.UserUseCase;
import com.jnu.ticketapi.dto.TokenDto;
import com.jnu.ticketcommon.exception.BadCredentialException;
import com.jnu.ticketdomain.domain.dto.LoginUserRequestDto;
import com.jnu.ticketdomain.domain.user.User;
import com.jnu.ticketdomain.persistence.UserPersistenceAdapter;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UserService implements UserUseCase {
    private final UserPersistenceAdapter userPersistenceAdapter;
    private final AuthService authService;
    private static final String SERVER = "Server";

    @Override
    public Optional<User> findByEmail(String email) {
        return userPersistenceAdapter.findByEmail(email);
    }

    @Override
    @Transactional
    public User save(User user) {
        return userPersistenceAdapter.save(user);
    }

    /*
    email로 유저를 찾아서 없으면 회원가입 후 토큰발급
    email로 유저를 찾아서 있으면 비밀번호가 맞는지 확인하고 맞으면 토큰발급
     */
    @Override
    @Transactional
    public TokenDto login(LoginUserRequestDto loginUserRequestDto) {
        BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
        Optional<User> userOptional = findByEmail(loginUserRequestDto.email());
        User user;
        if (userOptional.isEmpty()) {
            user = loginUserRequestDto.toEntity(loginUserRequestDto);
            save(user);

        } else {
            user = userOptional.get();
            if (!bCryptPasswordEncoder.matches(loginUserRequestDto.pwd(), user.getPwd())) {
                throw BadCredentialException.EXCEPTION;
            }
        }
        return authService.generateToken(SERVER, user.getEmail(), user.getUserRole().getValue());
    }
}
