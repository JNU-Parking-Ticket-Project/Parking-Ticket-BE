package com.jnu.ticketapi.registration;


import com.jnu.ticketapi.api.registration.model.request.FinalSaveRequest;

public class FinalSaveRequestTestDataBuilder {
    private String captchaCode = "1234";
    private String captchaAnswer = "1234";
    private String name = "박영규";
    private String affiliation = "AI융합대";
    private String department = "수학과";
    private String studentNum = "215551";
    private String carNum = "12나1234";
    private Boolean isLight = true;
    private String phoneNum = "010-1111-2222";
    private Long selectSectorId = 3L;

    public static FinalSaveRequestTestDataBuilder builder() {
        return new FinalSaveRequestTestDataBuilder();
    }

    public FinalSaveRequestTestDataBuilder withCaptchaCode(String captchaCode) {
        this.captchaCode = captchaCode;
        return this;
    }

    public FinalSaveRequestTestDataBuilder withCaptchaAnswer(String captchaAnswer) {
        this.captchaAnswer = captchaAnswer;
        return this;
    }

    public FinalSaveRequestTestDataBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public FinalSaveRequestTestDataBuilder withAffiliation(String affiliation) {
        this.affiliation = affiliation;
        return this;
    }

    public FinalSaveRequestTestDataBuilder withStudentNum(String studentNum) {
        this.studentNum = studentNum;
        return this;
    }

    public FinalSaveRequestTestDataBuilder withCarNum(String carNum) {
        this.carNum = carNum;
        return this;
    }

    public FinalSaveRequestTestDataBuilder withPhoneNumber(String phoneNum) {
        this.phoneNum = phoneNum;
        return this;
    }

    public FinalSaveRequestTestDataBuilder withIsLight(Boolean isLight) {
        this.isLight = isLight;
        return this;
    }

    public FinalSaveRequestTestDataBuilder withSelectSectorId(Long selectSectorId) {
        this.selectSectorId = selectSectorId;
        return this;
    }

    public FinalSaveRequestTestDataBuilder withDepartment(String department) {
        this.department = department;
        return this;
    }

    // 필요한 필드들에 대해 with 메서드를 추가
    public FinalSaveRequest build() {
        return FinalSaveRequest.builder()
                .captchaCode(captchaCode)
                .captchaAnswer(captchaAnswer)
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
