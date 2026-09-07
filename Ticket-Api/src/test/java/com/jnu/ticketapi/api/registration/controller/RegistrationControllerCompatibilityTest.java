package com.jnu.ticketapi.api.registration.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.jnu.ticketapi.api.registration.service.RegistrationUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class RegistrationControllerCompatibilityTest {

    @Test
    @DisplayName("기존 결과 집계 API는 결과를 다시 계산하지 않는 호환 응답을 반환한다")
    void keepsLegacyAssignResultEndpointWithoutMutation() {
        RegistrationUseCase registrationUseCase = mock(RegistrationUseCase.class);
        RegistrationController controller = new RegistrationController(registrationUseCase);

        var response = controller.assignResult(10L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("신청 결과는 신청 저장 시점에 확정됩니다.");
        verifyNoInteractions(registrationUseCase);
    }
}
