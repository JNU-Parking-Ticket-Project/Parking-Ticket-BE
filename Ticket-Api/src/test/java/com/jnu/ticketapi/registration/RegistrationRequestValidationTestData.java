package com.jnu.ticketapi.registration;

import static com.jnu.ticketapi.registration.FinalSaveRequestTestDataBuilder.builder;

import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;

final class RegistrationRequestValidationTestData {

    private static final String TOO_LONG = "가".repeat(256);

    private RegistrationRequestValidationTestData() {}

    static Stream<Arguments> invalidFinalRequests() {
        return Stream.of(
                Arguments.of("studentNum", builder().withStudentNum(TOO_LONG).build()),
                Arguments.of("affiliation", builder().withAffiliation(TOO_LONG).build()),
                Arguments.of("department", builder().withDepartment(TOO_LONG).build()),
                Arguments.of("carNum", builder().withCarNum(TOO_LONG).build()));
    }

    static Stream<Arguments> invalidTemporaryRequests() {
        return Stream.of(
                Arguments.of(
                        "studentNum",
                        TemporarySaveRequestTestDataBuilder.builder()
                                .withStudentNum(TOO_LONG)
                                .build()),
                Arguments.of(
                        "affiliation",
                        TemporarySaveRequestTestDataBuilder.builder()
                                .withAffiliation(TOO_LONG)
                                .build()),
                Arguments.of(
                        "department",
                        TemporarySaveRequestTestDataBuilder.builder()
                                .withDepartment(TOO_LONG)
                                .build()),
                Arguments.of(
                        "carNum",
                        TemporarySaveRequestTestDataBuilder.builder()
                                .withCarNum(TOO_LONG)
                                .build()));
    }
}
