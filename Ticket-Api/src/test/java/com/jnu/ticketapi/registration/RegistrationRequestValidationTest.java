package com.jnu.ticketapi.registration;

import static org.assertj.core.api.Assertions.assertThat;

import com.jnu.ticketapi.api.registration.model.request.FinalSaveRequest;
import com.jnu.ticketapi.api.registration.model.request.TemporarySaveRequest;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class RegistrationRequestValidationTest {

    private static javax.validation.ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @ParameterizedTest
    @MethodSource(
            "com.jnu.ticketapi.registration.RegistrationRequestValidationTestData#invalidFinalRequests")
    void finalSaveRejectsValuesThatCannotFitRegistrationColumns(
            String property, FinalSaveRequest request) {
        Set<ConstraintViolation<FinalSaveRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains(property);
    }

    @ParameterizedTest
    @MethodSource(
            "com.jnu.ticketapi.registration.RegistrationRequestValidationTestData#invalidTemporaryRequests")
    void temporarySaveRejectsValuesThatCannotFitRegistrationColumns(
            String property, TemporarySaveRequest request) {
        Set<ConstraintViolation<TemporarySaveRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains(property);
    }
}
