package com.jnu.ticketapi.api.council.model.request;


import com.jnu.ticketcommon.annotation.Password;
import com.jnu.ticketcommon.annotation.Phone;
import com.jnu.ticketcommon.message.ValidationMessage;
import com.jnu.ticketdomain.domains.council.domain.Council;
import com.jnu.ticketdomain.domains.user.domain.User;
import com.jnu.ticketdomain.domains.user.domain.UserRole;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import lombok.Builder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Builder
public record SignUpCouncilRequest(
        @Email(message = ValidationMessage.IS_NOT_VALID_EMAIL) String email,
        @Password(message = ValidationMessage.IS_NOT_VALID_PASSWORD) String pwd,
        @NotBlank(message = "이름은 " + ValidationMessage.MUST_NOT_BLANK) String name,
        @Phone(message = ValidationMessage.IS_NOT_VALID_PHONE) String phoneNum,
        @NotBlank(message = "학번은 " + ValidationMessage.MUST_NOT_BLANK) String studentNum) {
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
