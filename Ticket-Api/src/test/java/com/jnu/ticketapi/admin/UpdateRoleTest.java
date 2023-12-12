package com.jnu.ticketapi.admin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jnu.ticketapi.api.council.model.request.SignUpCouncilRequest;
import com.jnu.ticketapi.config.DatabaseClearExtension;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@AutoConfigureMockMvc
@ExtendWith(DatabaseClearExtension.class)
@WithMockUser(roles = "ADMIN")
public class UpdateRoleTest {
    @Autowired private MockMvc mvc;

    @Autowired private ObjectMapper om;

    @BeforeEach
    void setUp() throws Exception {
        // given
        SignUpCouncilRequest request =
                SignUpCouncilRequest.builder()
                        .email("ekrrdj07@naver.com")
                        .pwd("1234")
                        .phoneNum("010-2293-5028")
                        .studentNum("215555")
                        .name("이진혁")
                        .build();
        String requestBody = om.writeValueAsString(request);

        // when
        ResultActions resultActions =
                mvc.perform(
                        post("/v1/council/signup")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody));
    }

    @Nested
    class updateRoleTest {
        @Test
        @DisplayName("성공 : 권한 변경")
        void updateRoleTest() throws Exception {
            {
                // given
                Long userId = 1L;
                String jsonRequest = "{\"role\":\"COUNCIL\"}";
                log.info("requestBody : " + jsonRequest);

                // when
                ResultActions resultActions =
                        mvc.perform(
                                put("/v1/admin/role/" + userId)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(jsonRequest));

                // eye
                String responseBody = resultActions.andReturn().getResponse().getContentAsString();
                log.info("responseBody : " + responseBody);

                // then
                resultActions.andExpectAll(
                        status().isOk(),
                        jsonPath("$.userId").value(1L),
                        jsonPath("$.role").value("COUNCIL"));
            }
        }

        @Test
        @DisplayName("실패 : 권한 변경(존재하지 않는 유저 ID 입력)")
        void updateRoleFailTest() throws Exception {
            {
                // given
                Long userId = 55L;
                String jsonRequest = "{\"role\":\"COUNCIL\"}";
                log.info("requestBody : " + jsonRequest);

                // when
                ResultActions resultActions =
                        mvc.perform(
                                put("/v1/admin/role/" + userId)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(jsonRequest));

                // eye
                String responseBody = resultActions.andReturn().getResponse().getContentAsString();
                log.info("responseBody : " + responseBody);

                // then
                resultActions.andExpectAll(
                        status().isNotFound(),
                        jsonPath("$.success").value(false),
                        jsonPath("$.reason").value("존재하지 않는 유저 입니다."),
                        jsonPath("$.code").value("USER_404_1"));
            }
        }

        @Test
        @DisplayName("실패 : 권한 변경(존재하지 않는 권한 입력)")
        void updateRoleFailTest2() throws Exception {
            {
                // given
                Long userId = 1L;
                String jsonRequest = "{\"role\":\"WhySoSerious\"}";
                log.info("requestBody : " + jsonRequest);

                // when
                ResultActions resultActions =
                        mvc.perform(
                                put("/v1/admin/role/" + userId)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(jsonRequest));

                // eye
                String responseBody = resultActions.andReturn().getResponse().getContentAsString();
                log.info("responseBody : " + responseBody);

                // then
                resultActions.andExpectAll(
                        status().isBadRequest(),
                        jsonPath("$.success").value(false),
                        jsonPath("$.reason").value("ROLE을 정확히 입력 해주세요."),
                        jsonPath("$.code").value("BAD_REQUEST"));
            }
        }
    }
}
