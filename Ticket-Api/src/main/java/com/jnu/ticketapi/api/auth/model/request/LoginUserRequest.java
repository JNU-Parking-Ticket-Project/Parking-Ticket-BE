package com.jnu.ticketapi.api.auth.model.request;


import com.jnu.ticketcommon.annotation.Password;
import com.jnu.ticketcommon.message.ValidationMessage;
import com.jnu.ticketdomain.domains.user.domain.User;
import com.jnu.ticketdomain.domains.user.domain.UserRole;
import javax.validation.constraints.Email;
import lombok.Builder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Builder
public record LoginUserRequest(
        @Email(message = ValidationMessage.IS_NOT_VALID_EMAIL) String email,
        @Password(message = ValidationMessage.IS_NOT_VALID_PASSWORD) String pwd) {
    public User toEntity(LoginUserRequest loginUserRequest) {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        return User.builder()
                .email(loginUserRequest.email())
                .pwd(passwordEncoder.encode(loginUserRequest.pwd()))
                .userRole(UserRole.USER)
                .build();
    }
}
