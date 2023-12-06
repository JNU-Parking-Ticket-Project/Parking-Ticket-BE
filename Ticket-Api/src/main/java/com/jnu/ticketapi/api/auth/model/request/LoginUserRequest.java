package com.jnu.ticketapi.api.auth.model.request;


import com.jnu.ticketdomain.domains.user.domain.User;
import com.jnu.ticketdomain.domains.user.domain.UserRole;
import lombok.Builder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Builder
public record LoginUserRequest(String email, String pwd) {
    public User toEntity(LoginUserRequest loginUserRequest) {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        return User.builder()
                .email(loginUserRequest.email())
                .pwd(passwordEncoder.encode(loginUserRequest.pwd()))
                .userRole(UserRole.USER)
                .build();
    }
}
