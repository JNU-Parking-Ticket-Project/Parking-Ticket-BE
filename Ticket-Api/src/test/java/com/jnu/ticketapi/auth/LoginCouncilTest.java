package com.jnu.ticketapi.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jnu.ticketapi.api.auth.model.request.LoginCouncilRequest;
import com.jnu.ticketapi.api.council.model.request.SignUpCouncilRequest;
import com.jnu.ticketcommon.exception.GlobalErrorCode;
import com.jnu.ticketcommon.message.ValidationMessage;
import com.jnu.ticketdomain.domains.council.exception.CouncilErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Sql("classpath:db/teardown.sql")
public class LoginCouncilTest {
    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper om;

    @BeforeEach
    void setUp() throws Exception {
        //given
        SignUpCouncilRequest request =
                SignUpCouncilRequest.builder().email("ekrrdj21@jnu.ac.kr").name("이진혁").pwd("Dlwlsgur@123").studentNum("21555").phoneNum("010-000-0000").build();

        String requestBody = om.writeValueAsString(request);

        mvc.perform(
                post("/v1/council/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody));

        SignUpCouncilRequest request2 =
                SignUpCouncilRequest.builder().email("ekrrdj21@jnu.ac.kr").name("이진혁").pwd("Dlwlsgur@123").studentNum("21555").phoneNum("010-000-0000").build();
        String requestBody2 = om.writeValueAsString(request);


        mvc.perform(
                post("/v1/council/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody));
    }

    @Nested
    class loginCouncilTest {
        @Test
        @DisplayName("성공 : 학생회 로그인")
        void councilLoginTest() throws Exception {
            // given
            LoginCouncilRequest request =
                    LoginCouncilRequest.builder().email("council@jnu.ac.kr").pwd("Council@123").build();
            String requestBody = om.writeValueAsString(request);
            // when
            ResultActions resultActions =
                    mvc.perform(
                            post("/v1/auth/login/council")
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
            jsonPath("$.refreshToken").exists()
            );
            log.info("responseBody : " + responseBody);
        }

        @Test
        @DisplayName("실패 : 학생회 로그인(권한이 학생이 이상이 아닐경우)")
        void councilLoginTestFail() throws Exception {
            // given
            LoginCouncilRequest request =
                    LoginCouncilRequest.builder().email("ekrrdj21@jnu.ac.kr").pwd("Dlwlsgur@123").build();
            String requestBody = om.writeValueAsString(request);
            // when
            ResultActions resultActions =
                    mvc.perform(
                            post("/v1/auth/login/council")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(requestBody));
            // eye
            String responseBody = resultActions.andReturn().getResponse().getContentAsString();
            // then
            resultActions.andExpectAll(
                    status().is4xxClientError(),
                    jsonPath("$.status").value(400),
                    jsonPath("$.reason").value(CouncilErrorCode.IS_NOT_COUNCIL.getReason())
            );
            log.info("GlobalError코드: " + GlobalErrorCode.BAD_CREDENTIAL.getReason());
        }

        @Test
        @DisplayName("실패 : 학생회 로그인(비밀번호가 틀린 경우)")
        void councilLoginTestFail2() throws Exception {
            // given
            LoginCouncilRequest request =
                    LoginCouncilRequest.builder().email("ekrrdj21@jnu.ac.kr").pwd("Qkrdudrb@123").build();
            String requestBody = om.writeValueAsString(request);
            // when
            ResultActions resultActions =
                    mvc.perform(
                            post("/v1/auth/login/council")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(requestBody));
            // eye
            String responseBody = resultActions.andReturn().getResponse().getContentAsString();
            // then
            resultActions.andExpectAll(
                    status().is4xxClientError(),
                    jsonPath("$.status").value(400),
                    jsonPath("$.reason").value(GlobalErrorCode.BAD_CREDENTIAL.getReason())
            );
        }

        @Test
        @DisplayName("실패 : 학생회 로그인(비밀번호가 정규식에 맞지 않는경우)")
        void councilLoginTestFail3() throws Exception {

            // given
            LoginCouncilRequest request =
                    LoginCouncilRequest.builder().email("ekrrrdj21@jnu.ac.kr").pwd("1234").build();
            String requestBody = om.writeValueAsString(request);
            // when
            ResultActions resultActions =
                    mvc.perform(
                            post("/v1/auth/login/council")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(requestBody));
            // eye
            String responseBody = resultActions.andReturn().getResponse().getContentAsString();
            // then
            resultActions.andExpectAll(
                    status().is4xxClientError(),
                    jsonPath("$.status").value(400),
                    jsonPath("$.reason").value(ValidationMessage.IS_NOT_VALID_PASSWORD)
            );
            log.info("responseBody : " + responseBody);
        }

        @Test
        @DisplayName("실패 : 학생회 로그인(이메일이 이메일 형식에 맞지 않는경우)")
        void councilLoginTestFail4() throws Exception {

            // given
            LoginCouncilRequest request =
                    LoginCouncilRequest.builder().email("ekrrrdj21jnu.ac.kr").pwd("Dlwlsgur@123").build();
            String requestBody = om.writeValueAsString(request);
            // when
            ResultActions resultActions =
                    mvc.perform(
                            post("/v1/auth/login/council")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(requestBody));
            // eye
            String responseBody = resultActions.andReturn().getResponse().getContentAsString();
            // then
            resultActions.andExpectAll(
                    status().is4xxClientError(),
                    jsonPath("$.status").value(400),
                    jsonPath("$.reason").value(ValidationMessage.IS_NOT_VALID_EMAIL)
            );
            log.info("responseBody : " + responseBody);
        }
    }
}
