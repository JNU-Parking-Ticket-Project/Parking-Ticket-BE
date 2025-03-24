package com.jnu.ticketapi.registration;

import com.jnu.ticketapi.api.registration.model.request.TemporarySaveRequest;

public class TemporarySaveRequestTestDataBuilder {
    private String name = "박영규";
    private String affiliation = "AI융합대";
    private String department = "수학과";
    private String studentNum = "215551";
    private String carNum = "12나1234";
    private Boolean isLight = true;
    private String phoneNum = "010-1111-2222";
    private Long selectSectorId = 3L;

    public static TemporarySaveRequestTestDataBuilder builder() {
        return new TemporarySaveRequestTestDataBuilder();
    }

    public TemporarySaveRequestTestDataBuilder withSelectSectorId(Long selectSectorId) {
        this.selectSectorId = selectSectorId;
        return this;
    }

    public TemporarySaveRequestTestDataBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public TemporarySaveRequestTestDataBuilder withAffiliation(String affiliation) {
        this.affiliation = affiliation;
        return this;
    }

    public TemporarySaveRequestTestDataBuilder withStudentNum(String studentNum) {
        this.studentNum = studentNum;
        return this;
    }

    public TemporarySaveRequestTestDataBuilder withCarNum(String carNum) {
        this.carNum = carNum;
        return this;
    }

    public TemporarySaveRequestTestDataBuilder withPhoneNum(String phoneNum) {
        this.phoneNum = phoneNum;
        return this;
    }

    public TemporarySaveRequestTestDataBuilder withIsLight(Boolean isLight) {
        this.isLight = isLight;
        return this;
    }

    public TemporarySaveRequestTestDataBuilder withDepartment(String department) {
        this.department = department;
        return this;
    }

    public TemporarySaveRequest build() {
        return TemporarySaveRequest.builder()
                .name(name)
                .affiliation(affiliation)
                .department(department)
                .studentNum(studentNum)
                .carNum(carNum)
                .isLight(isLight)
                .phoneNum(phoneNum)
                .selectSectorId(selectSectorId)
                .build();
    }
}
