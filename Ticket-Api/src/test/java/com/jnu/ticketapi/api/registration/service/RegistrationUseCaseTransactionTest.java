package com.jnu.ticketapi.api.registration.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.jnu.ticketapi.api.registration.model.request.FinalSaveRequest;
import java.lang.reflect.Method;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

class RegistrationUseCaseTransactionTest {

    @Test
    @DisplayName("최종 신청은 journal의 REQUIRES_NEW 트랜잭션 밖에서 실행한다")
    void finalSaveSuspendsCallerTransaction() throws NoSuchMethodException {
        Method finalSave =
                RegistrationUseCase.class.getMethod(
                        "finalSave", FinalSaveRequest.class, String.class, Long.class);

        Transactional transactional = finalSave.getAnnotation(Transactional.class);

        assertThat(transactional).isNotNull();
        assertThat(transactional.propagation()).isEqualTo(Propagation.NOT_SUPPORTED);
    }
}
