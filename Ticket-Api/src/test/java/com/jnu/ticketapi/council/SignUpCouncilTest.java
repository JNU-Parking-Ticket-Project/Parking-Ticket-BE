package com.jnu.ticketapi.council;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jnu.ticketapi.RestDocsConfig;
import com.jnu.ticketapi.api.council.model.request.SignUpCouncilRequest;
import com.jnu.ticketcommon.message.ResponseMessage;
import com.jnu.ticketcommon.message.ValidationMessage;
import com.jnu.ticketdomain.domains.council.exception.CouncilErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@AutoConfigureMockMvc
@AutoConfigureRestDocs
@Sql("classpath:db/teardown.sql")
public class SignUpCouncilTest extends RestDocsConfig {
    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper om;

    @Nested
    class signUpCouncilTest {
        @Test
        @DisplayName("성공 : 학생회 회원가입")
        void success() throws Exception {
            //given
            SignUpCouncilRequest request =
                    SignUpCouncilRequest.builder()
                            .email("asd123@naver.com")
                            .studentNum("215555")
                            .phoneNum("010-000-0000")
                            .name("응애")
                            .pwd("User@1234")
                            .build();
            String requestBody = om.writeValueAsString(request);
            //when
            ResultActions resultActions =
                    mvc.perform(
                            post("/v1/council/signup")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(requestBody));
            // eye
            String responseBody = resultActions.andReturn().getResponse().getContentAsString();

            //then
            resultActions.andExpectAll(
                    status().isOk(),
                    jsonPath("$.message").value(ResponseMessage.SUCCESS_SIGN_UP));
            log.info("responseBody : {}", responseBody);
        }

        @Test
        @DisplayName("실패 : 학생회 회원가입(이메일이 중복인 경우)")
        void fail() throws Exception {
            //given
            SignUpCouncilRequest request =
                    SignUpCouncilRequest.builder()
                            .email("user@jnu.ac.kr")
                            .studentNum("215555")
                            .phoneNum("010-000-0000")
                            .name("응애")
                            .pwd("User@1234")
                            .build();
            String requestBody = om.writeValueAsString(request);
            //when
            ResultActions resultActions =
                    mvc.perform(
                            post("/v1/council/signup")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(requestBody));
            // eye
            String responseBody = resultActions.andReturn().getResponse().getContentAsString();

            //then
            resultActions.andExpectAll(
                    status().is4xxClientError(),
                    jsonPath("$.code").value(CouncilErrorCode.ALREADY_EXIST_EMAIL.getCode()),
                    jsonPath("$.reason").value(CouncilErrorCode.ALREADY_EXIST_EMAIL.getReason()));
            resultActions.andDo(MockMvcResultHandlers.print()).andDo(document);
            log.info("responseBody : {}", responseBody);
        }

        @Test
        @DisplayName("실패 : 학생회 회원가입(이름이 null 혹은 공백인 경우)")
        void fail2() throws Exception {
            //given
            SignUpCouncilRequest request =
                    SignUpCouncilRequest.builder()
                            .email("asd123@naver.com")
                            .studentNum("215555")
                            .phoneNum("010-000-0000")
                            .name(" ")
                            .pwd("User@1234")
                            .build();
            String requestBody = om.writeValueAsString(request);
            //when
            ResultActions resultActions =
                    mvc.perform(
                            post("/v1/council/signup")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(requestBody));
            // eye
            String responseBody = resultActions.andReturn().getResponse().getContentAsString();

            //then
            resultActions.andExpectAll(
                    status().is4xxClientError(),
                    jsonPath("$.reason").value("이름은 " + ValidationMessage.MUST_NOT_BLANK),
                    jsonPath("$.code").value("BAD_REQUEST"));
            resultActions.andDo(MockMvcResultHandlers.print()).andDo(document);
            log.info("responseBody : {}", responseBody);
        }

        @Test
        @DisplayName("실패 : 학생회 회원가입(이메일이 이메일 형식에 맞지 않는경우)")
        void fail3() throws Exception {
            //given
            SignUpCouncilRequest request =
                    SignUpCouncilRequest.builder()
                            .email("ImFaker")
                            .studentNum("215555")
                            .phoneNum("010-000-0000")
                            .name("응애")
                            .pwd("User@1234")
                            .build();
            String requestBody = om.writeValueAsString(request);
            //when
            ResultActions resultActions =
                    mvc.perform(
                            post("/v1/council/signup")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(requestBody));
            // eye
            String responseBody = resultActions.andReturn().getResponse().getContentAsString();

            //then
            resultActions.andExpectAll(
                    status().is4xxClientError(),
                    jsonPath("$.status").value(400),
                    jsonPath("$.reason").value(ValidationMessage.IS_NOT_VALID_EMAIL));
            resultActions.andDo(MockMvcResultHandlers.print()).andDo(document);
            log.info("responseBody : " + responseBody);
        }

        @Test
        @DisplayName("실패 : 학생회 회원가입(비밀번호가 정규식에 맞지 않는경우)")
        void fail4() throws Exception {
            //given
            SignUpCouncilRequest request =
                    SignUpCouncilRequest.builder()
                            .email("asd123@naver.com")
                            .studentNum("215555")
                            .phoneNum("010-000-0000")
                            .name("응애")
                            .pwd("1234")
                            .build();
            String requestBody = om.writeValueAsString(request);
            //when
            ResultActions resultActions =
                    mvc.perform(
                            post("/v1/council/signup")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(requestBody));
            // eye
            String responseBody = resultActions.andReturn().getResponse().getContentAsString();

            //then
            resultActions.andExpectAll(
                    status().is4xxClientError(),
                    jsonPath("$.status").value(400),
                    jsonPath("$.reason").value(ValidationMessage.IS_NOT_VALID_PASSWORD));
            resultActions.andDo(MockMvcResultHandlers.print()).andDo(document);
            log.info("responseBody : " + responseBody);
        }

        @Test
        @DisplayName("실패 : 학생회 회원가입(전화번호가 전화번호 형식이 아닌경우)")
        void fail5() throws Exception {
            //given
            SignUpCouncilRequest request =
                    SignUpCouncilRequest.builder()
                            .email("asd123@naver.com")
                            .studentNum("215555")
                            .phoneNum("010")
                            .name("응애")
                            .pwd("User@1234")
                            .build();
            String requestBody = om.writeValueAsString(request);
            //when
            ResultActions resultActions =
                    mvc.perform(
                            post("/v1/council/signup")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(requestBody));
            // eye
            String responseBody = resultActions.andReturn().getResponse().getContentAsString();

            //then
            resultActions.andExpectAll(
                    status().is4xxClientError(),
                    jsonPath("$.reason").value(ValidationMessage.IS_NOT_VALID_PHONE),
                    jsonPath("$.code").value("BAD_REQUEST"));
            resultActions.andDo(MockMvcResultHandlers.print()).andDo(document);
            log.info("responseBody : " + responseBody);
        }

        @Test
        @DisplayName("실패 : 학생회 회원가입(학번이 null 혹은 공백인 경우)")
        void fail6() throws Exception {
            //given
            SignUpCouncilRequest request =
                    SignUpCouncilRequest.builder()
                            .email("asd123@naver.com")
                            .studentNum(" ")
                            .phoneNum("010-000-0000")
                            .name("응애")
                            .pwd("User@1234")
                            .build();
            String requestBody = om.writeValueAsString(request);
            //when
            ResultActions resultActions =
                    mvc.perform(
                            post("/v1/council/signup")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(requestBody));
            // eye
            String responseBody = resultActions.andReturn().getResponse().getContentAsString();

            //then
            resultActions.andExpectAll(
                    status().is4xxClientError(),
                    jsonPath("$.reason").value("학번은 " + ValidationMessage.MUST_NOT_BLANK),
                    jsonPath("$.code").value("BAD_REQUEST"));
            resultActions.andDo(MockMvcResultHandlers.print()).andDo(document);
            log.info("responseBody : " + responseBody);
        }
    }
}
