package com.jnu.ticketdomain.domain.dto;


import com.jnu.ticketdomain.domain.user.User;
import com.jnu.ticketdomain.domain.user.UserRole;
import lombok.Builder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Builder
public record LoginUserRequestDto(String email, String pwd) {
    public User toEntity(LoginUserRequestDto loginUserRequestDto) {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        return User.builder()
                .email(loginUserRequestDto.email())
                .pwd(passwordEncoder.encode(loginUserRequestDto.pwd()))
                .userRole(UserRole.USER)
                .build();
    }
}
