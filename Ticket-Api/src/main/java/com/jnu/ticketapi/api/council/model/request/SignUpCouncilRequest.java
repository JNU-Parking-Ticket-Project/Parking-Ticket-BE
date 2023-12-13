package com.jnu.ticketapi.api.council.model.request;


import com.jnu.ticketdomain.domains.council.domain.Council;
import com.jnu.ticketdomain.domains.user.domain.User;
import com.jnu.ticketdomain.domains.user.domain.UserRole;
import lombok.Builder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
@Builder
public record SignUpCouncilRequest(
        String email, String pwd, String name, String phoneNum, String studentNum) {
    public User toUserEntity(SignUpCouncilRequest signUpCouncilRequest) {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        return User.builder()
                .email(signUpCouncilRequest.email())
                .pwd(passwordEncoder.encode(signUpCouncilRequest.pwd()))
                .userRole(UserRole.USER)
                .build();
    }

    public Council toCouncilEntity(SignUpCouncilRequest signUpCouncilRequest, User user) {
        return Council.builder()
                .name(signUpCouncilRequest.name())
                .phoneNum(signUpCouncilRequest.phoneNum())
                .user(user)
                .studentNum(signUpCouncilRequest.studentNum())
                .build();
    }
}
