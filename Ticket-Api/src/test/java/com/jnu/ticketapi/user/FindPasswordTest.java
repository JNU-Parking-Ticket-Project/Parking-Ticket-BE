package com.jnu.ticketapi.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jnu.ticketapi.config.DatabaseClearExtension;
import com.jnu.ticketapi.api.auth.model.request.LoginUserRequest;
import com.jnu.ticketapi.api.user.model.request.FindPasswordRequest;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
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
public class FindPasswordTest {

    @Autowired private MockMvc mvc;

    @Autowired private ObjectMapper om;

    @Test
    @DisplayName("성공 : 비밀번호 찾기 메일 전송")
    @WithMockUser(roles = "COUNCIL")
    void find_password_test() throws Exception {
        // given
        LoginUserRequest requestsDto =
                LoginUserRequest.builder().email("pon05114@naver.com").pwd("1234").build();
        String testRequestBody = om.writeValueAsString(requestsDto);
        mvc.perform(
                post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(testRequestBody));

        FindPasswordRequest findPasswordRequest =
                FindPasswordRequest.builder().email("pon05114@naver.com").build();
        String requestBody = om.writeValueAsString(findPasswordRequest);

        // when
        ResultActions resultActions =
                mvc.perform(
                        post("/v1/user/password/find")
                                .contentType(MediaType.APPLICATION_JSON)
                                .characterEncoding(StandardCharsets.UTF_8)
                                .content(requestBody));

        // eye
        String responseBody =
                resultActions.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        log.info("responseBody : " + responseBody);

        // then
        resultActions.andExpectAll(status().isOk());
    }
}
