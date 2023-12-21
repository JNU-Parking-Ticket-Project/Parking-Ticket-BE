package com.jnu.ticketapi.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jnu.ticketapi.RestDocsConfig;
import com.jnu.ticketapi.api.auth.model.request.LoginUserRequest;
import com.jnu.ticketapi.config.DatabaseClearExtension;
import com.jnu.ticketcommon.exception.GlobalErrorCode;
import com.jnu.ticketcommon.message.ValidationMessage;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@AutoConfigureMockMvc
@AutoConfigureRestDocs
@ExtendWith(DatabaseClearExtension.class)
public class LoginTest extends RestDocsConfig {

    @Autowired private MockMvc mvc;

    @Autowired private ObjectMapper om;

    @BeforeEach
    void setUp() throws Exception {
        // given
        LoginUserRequest request =
                LoginUserRequest.builder()
                        .email("ekrrrdj123@jnu.ac.kr")
                        .pwd("Dlwlsgur@123")
                        .build();
        String requestBody = om.writeValueAsString(request);
        // when
        ResultActions resultActions =
                mvc.perform(
                        post("/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody));
    }

    @Nested
    class loginTest {
        @Test
        @DisplayName("성공 : 로그인/회원가입")
        void success() throws Exception {

            // given
            LoginUserRequest request =
                    LoginUserRequest.builder()
                            .email("ekrrrdj21@jnu.ac.kr")
                            .pwd("Dlwlsgur@123")
                            .build();
            String requestBody = om.writeValueAsString(request);
            // when
            ResultActions resultActions =
                    mvc.perform(
                            post("/v1/auth/login")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(requestBody));
            // eye
            String responseBody = resultActions.andReturn().getResponse().getContentAsString();
            // then
            resultActions.andExpectAll(
                    status().isOk(),
                    /*
                    accessToken, refreshToken 요청마다 새로 발급되서 예측을 할 수 없어서 exists()로 검사
                    */
                    jsonPath("$.accessToken").exists(),
                    jsonPath("$.refreshToken").exists());
            resultActions.andDo(MockMvcResultHandlers.print()).andDo(document);
            log.info("responseBody : " + responseBody);
        }

        @Test
        @DisplayName("실패 : 로그인/회원가입(비밀번호 틀린 경우)")
        void fail() throws Exception {
            // given
            LoginUserRequest request =
                    LoginUserRequest.builder()
                            .email("ekrrrdj123@jnu.ac.kr")
                            .pwd("Qkrdudrb@123")
                            .build();
            String requestBody = om.writeValueAsString(request);
            // when
            ResultActions resultActions =
                    mvc.perform(
                            post("/v1/auth/login")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(requestBody));
            // eye
            String responseBody = resultActions.andReturn().getResponse().getContentAsString();
            // then
            resultActions.andExpectAll(
                    status().is4xxClientError(),
                    jsonPath("$.status").value(400),
                    jsonPath("$.reason").value(GlobalErrorCode.BAD_CREDENTIAL.getReason()));
            resultActions.andDo(MockMvcResultHandlers.print()).andDo(document);
            log.info("responseBody : " + responseBody);
        }

        @Test
        @DisplayName("실패 : 로그인/회원가입(비밀번호가 정규식에 맞지 않는경우)")
        void fail2() throws Exception {

            // given
            LoginUserRequest request =
                    LoginUserRequest.builder().email("ekrrrdj21@jnu.ac.kr").pwd("1234").build();
            String requestBody = om.writeValueAsString(request);
            // when
            ResultActions resultActions =
                    mvc.perform(
                            post("/v1/auth/login")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(requestBody));
            // eye
            String responseBody = resultActions.andReturn().getResponse().getContentAsString();
            // then
            resultActions.andExpectAll(
                    status().is4xxClientError(),
                    jsonPath("$.status").value(400),
                    jsonPath("$.reason").value(ValidationMessage.IS_NOT_VALID_PASSWORD));
            resultActions.andDo(MockMvcResultHandlers.print()).andDo(document);
            log.info("responseBody : " + responseBody);
        }

        @Test
        @DisplayName("실패 : 로그인/회원가입(이메일이 이메일 형식에 맞지 않는경우)")
        void fail3() throws Exception {

            // given
            LoginUserRequest request =
                    LoginUserRequest.builder()
                            .email("ekrrrdj21jnu.ac.kr")
                            .pwd("Dlwlsgur@123")
                            .build();
            String requestBody = om.writeValueAsString(request);
            // when
            ResultActions resultActions =
                    mvc.perform(
                            post("/v1/auth/login")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(requestBody));
            // eye
            String responseBody = resultActions.andReturn().getResponse().getContentAsString();
            // then
            resultActions.andExpectAll(
                    status().is4xxClientError(),
                    jsonPath("$.status").value(400),
                    jsonPath("$.reason").value(ValidationMessage.IS_NOT_VALID_EMAIL));
            resultActions.andDo(MockMvcResultHandlers.print()).andDo(document);
            log.info("responseBody : " + responseBody);
        }
    }
}
