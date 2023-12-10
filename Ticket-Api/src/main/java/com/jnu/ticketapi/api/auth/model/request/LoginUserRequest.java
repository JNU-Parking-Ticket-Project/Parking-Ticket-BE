package com.jnu.ticketapi.api.auth.model.request;


import com.jnu.ticketdomain.domains.user.domain.User;
import com.jnu.ticketdomain.domains.user.domain.UserRole;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Builder
public record LoginUserRequest(@Email String email, @NotNull String pwd) {
    public User toEntity(LoginUserRequest loginUserRequest) {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        return User.builder()
                .email(loginUserRequest.email())
                .pwd(passwordEncoder.encode(loginUserRequest.pwd()))
                .userRole(UserRole.USER)
                .build();
    }
}
