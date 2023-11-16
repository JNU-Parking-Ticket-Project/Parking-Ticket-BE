package com.jnu.ticketdomain.domains.dto;


import com.jnu.ticketdomain.domains.user.domian.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public record LoginUserRqDto(String email, String pwd) {
    public User toEntity(LoginUserRqDto loginUserRqDto) {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        return User.builder()
                .email(loginUserRqDto.email())
                .pwd(passwordEncoder.encode(loginUserRqDto.pwd()))
                .build();
    }
}
