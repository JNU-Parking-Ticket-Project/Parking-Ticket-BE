package com.jnu.ticketdomain.fixture;

import com.jnu.ticketdomain.domains.user.domain.User;
import com.jnu.ticketdomain.domains.user.domain.UserRole;
import com.jnu.ticketdomain.domains.user.domain.UserStatus;

public class UserTestBuilder {

    private String pwd = "password";
    private String email = "test@example.com";
    private UserRole userRole = UserRole.USER;
    private UserStatus status = UserStatus.FAIL;
    private Integer sequence = -1;

    public static UserTestBuilder builder() {
        return new UserTestBuilder();
    }

    public UserTestBuilder withEmail(String email) {
        this.email = email;
        return this;
    }

    public UserTestBuilder withPassword(String pwd) {
        this.pwd = pwd;
        return this;
    }

    public UserTestBuilder asSuccess() {
        this.status = UserStatus.SUCCESS;
        this.sequence = -2;
        return this;
    }

    public UserTestBuilder asPrepare(int sequence) {
        this.status = UserStatus.PREPARE;
        this.sequence = sequence;
        return this;
    }

    public UserTestBuilder asFail() {
        this.status = UserStatus.FAIL;
        this.sequence = -1;
        return this;
    }

    public User build() {
        User user = User.builder()
                .email(email)
                .pwd(pwd)
                .userRole(userRole)
                .build();

        // 상태 세팅
        switch (status) {
            case SUCCESS -> user.success();
            case FAIL -> user.fail();
            case PREPARE -> user.prepare(sequence);
        }

        return user;
    }
}
