package com.jnu.ticketinfrastructure.model;

import com.jnu.ticketdomain.domains.registration.domain.Registration;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@ToString
public class RegistrationInfo {

    private Long id;
    private String email;
    private String name;
    private String studentNum;
    private String affiliation;
    private String department;
    private String carNum;
    private boolean light;
    private String phoneNum;
    private String createdAt;
    private boolean saved;
    private boolean deleted;
    private Long savedAt;
    private Long userId;
    private Long sectorId;
    private Long eventId;

    protected RegistrationInfo() {
    }

    public RegistrationInfo(Registration registration) {
        this.id = registration.getId();
        this.email = registration.getEmail();
        this.name = registration.getName();
        this.studentNum = registration.getStudentNum();
        this.affiliation = registration.getAffiliation();
        this.department = registration.getDepartment();
        this.carNum = registration.getCarNum();
        this.light = registration.isLight();
        this.phoneNum = registration.getPhoneNum();
        this.createdAt = getCreatedAt(registration.getCreatedAt());
        this.saved = registration.isSaved();
        this.deleted = registration.isDeleted();
        this.savedAt = registration.getSavedAt();
        this.userId = registration.getUser().getId();
        this.sectorId = registration.getSector().getId();
        this.eventId = registration.getEventId();
    }

    private String getCreatedAt(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return localDateTime.toString();
    }
}
