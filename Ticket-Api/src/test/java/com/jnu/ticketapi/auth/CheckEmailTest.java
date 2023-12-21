package com.jnu.ticketapi.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jnu.ticketapi.RestDocsConfig;
import com.jnu.ticketapi.api.auth.model.request.CheckEmailRequest;
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

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@AutoConfigureMockMvc
@AutoConfigureRestDocs
@Sql("classpath:db/teardown.sql")
public class CheckEmailTest extends RestDocsConfig {
    @Autowired private MockMvc mvc;

    @Autowired private ObjectMapper om;

    @Nested
    class checkEmailTest {
        @Test
        @DisplayName("성공 : 이메일 중복 체크")
        void success() throws Exception {

            // given
            CheckEmailRequest request =
                    CheckEmailRequest.builder().email("ekrrdj07@jnu.ac.kr").build();
            String requestBody = om.writeValueAsString(request);
            // when
            ResultActions resultActions =
                    mvc.perform(
                            post("/v1/auth/check/email")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(requestBody));
            // eye
            String responseBody = resultActions.andReturn().getResponse().getContentAsString();
            // then
            resultActions.andExpectAll(
                    status().isOk(),
                    jsonPath("$.message").value(ResponseMessage.IS_POSSIBLE_EMAIL));
            resultActions.andDo(MockMvcResultHandlers.print()).andDo(document);
            log.info("responseBody : " + responseBody);
        }

        @Test
        @DisplayName("실패 : 이메일 중복 체크(이메일이 이미 존재하는 경우)")
        void fail() throws Exception {

            // given
            CheckEmailRequest request =
                    CheckEmailRequest.builder().email("council@jnu.ac.kr").build();
            String requestBody = om.writeValueAsString(request);
            // when
            ResultActions resultActions =
                    mvc.perform(
                            post("/v1/auth/check/email")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(requestBody));
            // eye
            String responseBody = resultActions.andReturn().getResponse().getContentAsString();
            // then
            resultActions.andExpectAll(
                    status().is4xxClientError(),
                    jsonPath("$.status").value(400),
                    jsonPath("$.reason").value(CouncilErrorCode.ALREADY_EXIST_EMAIL.getReason()));
            resultActions.andDo(MockMvcResultHandlers.print()).andDo(document);
            log.info("responseBody : " + responseBody);
        }

        @Test
        @DisplayName("실패 : 이메일 중복 체크(이메일이 이메일 형식에 맞지 않는경우)")
        void fail2() throws Exception {

            // given
            CheckEmailRequest request =
                    CheckEmailRequest.builder().email("ekrrrdj21jnu.ac.kr").build();
            String requestBody = om.writeValueAsString(request);
            // when
            ResultActions resultActions =
                    mvc.perform(
                            post("/v1/auth/check/email")
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
