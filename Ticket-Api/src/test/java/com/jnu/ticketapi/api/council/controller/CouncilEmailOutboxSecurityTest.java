package com.jnu.ticketapi.api.council.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jnu.ticketapi.api.council.model.response.FailedEmailOutboxesResponse;
import com.jnu.ticketapi.api.council.model.response.RequeueEmailOutboxResponse;
import com.jnu.ticketapi.api.council.service.CouncilUseCase;
import com.jnu.ticketapi.api.council.service.EmailOutboxRecoveryUseCase;
import com.jnu.ticketapi.config.SecurityConfig;
import com.jnu.ticketapi.config.response.JwtAccessDeniedHandler;
import com.jnu.ticketapi.config.response.JwtAuthenticationEntryPoint;
import com.jnu.ticketapi.security.JwtResolver;
import com.jnu.ticketcommon.helper.SpringEnvironmentHelper;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CouncilController.class)
@ContextConfiguration(classes = CouncilController.class)
@ActiveProfiles("test")
@Import({
    SecurityConfig.class,
    JwtAccessDeniedHandler.class,
    JwtAuthenticationEntryPoint.class,
    SpringEnvironmentHelper.class
})
class CouncilEmailOutboxSecurityTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private CouncilUseCase councilUseCase;
    @MockBean private EmailOutboxRecoveryUseCase emailOutboxRecoveryUseCase;
    @MockBean private JwtResolver jwtResolver;

    @Test
    @DisplayName("미인증 사용자는 최종 실패 Outbox를 조회할 수 없다")
    void unauthenticatedUserCannotReadFailedOutboxes() throws Exception {
        mockMvc.perform(get("/v1/council/events/10/email-outboxes/failed"))
                .andExpect(status().isUnauthorized());

        verify(emailOutboxRecoveryUseCase, never()).findFailed(any(), any());
    }

    @Test
    @DisplayName("일반 사용자는 최종 실패 Outbox를 조회할 수 없다")
    void userCannotReadFailedOutboxes() throws Exception {
        authenticate("user-token", "USER");

        mockMvc.perform(
                        get("/v1/council/events/10/email-outboxes/failed")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer user-token"))
                .andExpect(status().isForbidden());

        verify(emailOutboxRecoveryUseCase, never()).findFailed(any(), any());
    }

    @Test
    @DisplayName("학생회 권한은 최종 실패 Outbox를 조회할 수 있다")
    void councilCanReadFailedOutboxes() throws Exception {
        authenticate("council-token", "COUNCIL");
        when(emailOutboxRecoveryUseCase.findFailed(eq(10L), any(Pageable.class)))
                .thenReturn(new FailedEmailOutboxesResponse(List.of(), 0, 20, 0, 0));

        mockMvc.perform(
                        get("/v1/council/events/10/email-outboxes/failed")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer council-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outboxes").isArray())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("관리자 권한은 최종 실패 Outbox를 재처리 상태로 복원할 수 있다")
    void adminCanRequeueFailedOutbox() throws Exception {
        authenticate("admin-token", "ADMIN");
        when(emailOutboxRecoveryUseCase.requeue(10L, 1L))
                .thenReturn(RequeueEmailOutboxResponse.pending(10L, 1L));

        mockMvc.perform(
                        post("/v1/council/events/10/email-outboxes/1/requeue")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outboxId").value(1))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    private void authenticate(String token, String role) {
        when(jwtResolver.extractToken("Bearer " + token)).thenReturn(token);
        when(jwtResolver.accessTokenValidateToken(token)).thenReturn(true);
        when(jwtResolver.getAuthentication(token))
                .thenReturn(
                        new UsernamePasswordAuthenticationToken(
                                role, "", List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }
}
