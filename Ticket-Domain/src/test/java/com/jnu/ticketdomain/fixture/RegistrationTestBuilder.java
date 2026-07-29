package com.jnu.ticketdomain.fixture;

import com.jnu.ticketdomain.domains.events.domain.Sector;
import com.jnu.ticketdomain.domains.registration.domain.Registration;
import com.jnu.ticketdomain.domains.user.domain.User;
import com.jnu.ticketdomain.domains.user.domain.UserStatus;

import java.time.LocalDateTime;

public class RegistrationTestBuilder {

    private String email = "student@example.com";
    private String name = "홍길동";
    private String studentNum = "202312345";
    private String affiliation = "공과대학";
    private String department = "컴퓨터공학과";
    private String carNum = "12가1234";
    private boolean isLight = false;
    private String phoneNum = "010-1234-5678";
    private LocalDateTime createdAt = LocalDateTime.now();
    private boolean isSaved = true;
    private boolean isDeleted = false;
    private Long savedAt = System.nanoTime();
    private Integer position;
    private UserStatus resultStatus;
    private Integer sequence;
    private User user;
    private Sector sector;
    private Long eventId = 1L;

    public static RegistrationTestBuilder builder() {
        return new RegistrationTestBuilder();
    }

    public RegistrationTestBuilder withUser(User user) {
        this.user = user;
        return this;
    }

    public RegistrationTestBuilder withSector(Sector sector) {
        this.sector = sector;
        return this;
    }

    public RegistrationTestBuilder asUnsaved() {
        this.isSaved = false;
        this.savedAt = null;
        return this;
    }

    public RegistrationTestBuilder asDeleted() {
        this.isDeleted = true;
        return this;
    }

    public RegistrationTestBuilder withStudentNum(String num) {
        this.studentNum = num;
        return this;
    }

    public RegistrationTestBuilder withEmail(String email) {
        this.email = email;
        return this;
    }

    public RegistrationTestBuilder withSavedAt(Long savedAt) {
        this.savedAt = savedAt;
        return this;
    }

    public RegistrationTestBuilder withEventId(Long eventId) {
        this.eventId = eventId;
        return this;
    }

    public RegistrationTestBuilder withResult(
            Integer position, UserStatus resultStatus, Integer sequence) {
        this.position = position;
        this.resultStatus = resultStatus;
        this.sequence = sequence;
        return this;
    }

    public Registration build() {
        return Registration.builder()
                .email(email)
                .name(name)
                .studentNum(studentNum)
                .affiliation(affiliation)
                .department(department)
                .carNum(carNum)
                .isLight(isLight)
                .phoneNum(phoneNum)
                .createdAt(createdAt)
                .isSaved(isSaved)
                .savedAt(savedAt)
                .position(position)
                .resultStatus(resultStatus)
                .sequence(sequence)
                .user(user)
                .sector(sector)
                .eventId(eventId)
                .build();
    }
}
