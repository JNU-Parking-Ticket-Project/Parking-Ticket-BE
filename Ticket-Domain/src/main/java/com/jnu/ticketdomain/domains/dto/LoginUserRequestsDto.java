package com.jnu.ticketdomain.domains.dto;


import com.jnu.ticketdomain.domains.user.domian.User;
import lombok.Builder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
@Builder
public record LoginUserRequestsDto(String email, String pwd) {
    public User toEntity(LoginUserRequestsDto loginUserRequestsDto) {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        return User.builder()
                .email(loginUserRequestsDto.email())
                .pwd(passwordEncoder.encode(loginUserRequestsDto.pwd()))
                .build();
    }
}
