package com.jnu.ticketapi.registration;

import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jnu.ticketapi.RestDocsConfig;
import com.jnu.ticketapi.api.captcha.model.response.CaptchaResponse;
import com.jnu.ticketapi.api.captcha.service.CaptchaHashProcessor;
import com.jnu.ticketapi.api.captcha.service.vo.HashResult;
import com.jnu.ticketapi.api.registration.model.request.FinalSaveRequest;
import com.jnu.ticketapi.security.JwtGenerator;
import com.jnu.ticketcommon.message.ValidationMessage;
import com.jnu.ticketdomain.domains.captcha.exception.CaptchaErrorCode;
import com.jnu.ticketdomain.domains.events.exception.SectorErrorCode;
import com.jnu.ticketdomain.domains.user.exception.UserErrorCode;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@AutoConfigureMockMvc
@AutoConfigureRestDocs
@Sql("classpath:db/teardown.sql")
public class FinalSaveTest extends RestDocsConfig {

    @Autowired private MockMvc mvc;

    @Autowired private ObjectMapper om;

    @Autowired JwtGenerator jwtGenerator;

    @Autowired CaptchaHashProcessor captchaHashProcessor;

    @Nested
    class finalSaveTest {
        @Test
        @DisplayName("성공 : 1차 신청")
        void success() throws Exception {
            // given
            String email = "admin@jnu.ac.kr";
            String captchaAnswer = "1234";
            String accessToken = jwtGenerator.generateAccessToken(email, "ADMIN");

            String captchaCode = getCaptchaCodeRequest(accessToken);

            FinalSaveRequest request =
                    FinalSaveRequest.builder()
                            .captchaCode(captchaCode)
                            .captchaAnswer(captchaAnswer)
                            .name("박영규")
                            .affiliation("AI융합대")
                            .studentNum("215551")
                            .carNum("12나1234")
                            .isLight(true)
                            .phoneNum("010-1111-2222")
                            .selectSectorId(3L)
                            .build();
            String requestBody = om.writeValueAsString(request);

            // when
            ResultActions resultActions =
                    mvc.perform(
                            post("/v1/registration/1")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .header("Authorization", "Bearer " + accessToken)
                                    .content(requestBody));

            // eye
            String responseBody = resultActions.andReturn().getResponse().getContentAsString();

            // then
            resultActions.andExpectAll(status().isOk(), jsonPath("$.email").value(email));
            resultActions.andDo(MockMvcResultHandlers.print()).andDo(document);
            log.info("responseBody : {}", responseBody);
        }

        private String getCaptchaCodeRequest(String accessToken) throws Exception {
            MvcResult captchaResult = mvc.perform(
                            get("/v1/captcha")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andReturn();

            String captchaResponseBody = captchaResult.getResponse().getContentAsString();
            CaptchaResponse captchaResponse = om.readValue(captchaResponseBody, CaptchaResponse.class);
            return captchaResponse.captchaCode();
        }

        @Test
        @DisplayName("실패 : 1차 신청(캡챠 정답이 틀렸을 경우)")
        void fail() throws Exception {
            // given
            String email = "admin@jnu.ac.kr";
            String captchaAnswer = "45";
            String accessToken = jwtGenerator.generateAccessToken(email, "ADMIN");

            String captchaCode = getCaptchaCodeRequest(accessToken);

            FinalSaveRequest request =
                    FinalSaveRequest.builder()
                            .captchaCode(captchaCode)
                            .captchaAnswer(captchaAnswer)
                            .name("박영규")
                            .affiliation("AI융합대")
                            .studentNum("215551")
                            .carNum("12나1234")
                            .isLight(true)
                            .phoneNum("010-1111-2222")
                            .selectSectorId(3L)
                            .build();
            String requestBody = om.writeValueAsString(request);

            // when
            ResultActions resultActions =
                    mvc.perform(
                            post("/v1/registration/1")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .header("Authorization", "Bearer " + accessToken)
                                    .content(requestBody));

            // eye
            String responseBody = resultActions.andReturn().getResponse().getContentAsString();

            // then
            resultActions.andExpectAll(
                    status().is4xxClientError(),
                    jsonPath("$.reason").value(CaptchaErrorCode.WRONG_CAPTCHA_ANSWER.getReason()));
            resultActions.andDo(MockMvcResultHandlers.print()).andDo(document);
            log.info("responseBody : {}", responseBody);
        }

        @Test
        @DisplayName("실패: 1차 신청(캡챠를 요청하지 않은 경우)")
        void fail15() throws Exception {
            //given
            String email = "user@jnu.ac.kr";
            String accessToken = jwtGenerator.generateAccessToken(email, "USER");

            FinalSaveRequest request = FinalSaveRequest.builder()
                    .captchaCode("testCaptchaCode")
                    .captchaAnswer("testCaptchaAnswer")
                    .name("박영규")
                    .affiliation("AI융합대")
                    .studentNum("215551")
                    .carNum("12나1234")
                    .isLight(true)
                    .phoneNum("010-1111-2222")
                    .selectSectorId(3L)
                    .build();

            //when
            ResultActions resultActions = mvc.perform(
                    post("/v1/registration/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + accessToken)
                            .content(om.writeValueAsString(request)));

            // eye
            String responseBody = resultActions.andReturn().getResponse().getContentAsString();

            //then
            resultActions.andExpectAll(
                    status().is4xxClientError(),
                    jsonPath("$.reason").value(CaptchaErrorCode.NOT_FOUND_CAPTCHA_LOG.getReason()));
            resultActions.andDo(MockMvcResultHandlers.print()).andDo(document);
            log.info("responseBody : {}", responseBody);
        }

        @Test
        @DisplayName("실패 : 1차 신청(요청하는 사용자를 찾을 수 없는 경우)")
        void fail2() throws Exception {
            // given
            String email = "imFaker@T1.com";
            HashResult result = captchaHashProcessor.hash(1L);
            String captchaAnswer = "1234";

            String accessToken = jwtGenerator.generateAccessToken(email, "ADMIN");

            FinalSaveRequest request =
                    FinalSaveRequest.builder()
                            .captchaCode(result.getCaptchaCode())
                            .captchaAnswer(captchaAnswer)
                            .name("박영규")
                            .affiliation("AI융합대")
                            .studentNum("215551")
                            .carNum("12나1234")
                            .isLight(true)
                            .phoneNum("010-1111-2222")
                            .selectSectorId(3L)
                            .build();
            String requestBody = om.writeValueAsString(request);

            // when
            ResultActions resultActions =
                    mvc.perform(
                            post("/v1/registration/1")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .header("Authorization", "Bearer " + accessToken)
                                    .content(requestBody));

            // eye
            String responseBody = resultActions.andReturn().getResponse().getContentAsString();

            // then
            resultActions.andExpectAll(
                    status().isNotFound(),
                    jsonPath("$.reason").value(UserErrorCode.NOT_FOUND_USER.getReason()),
                    jsonPath("$.code").value(UserErrorCode.NOT_FOUND_USER.getCode()));
            resultActions.andDo(MockMvcResultHandlers.print()).andDo(document);
            log.info("responseBody : {}", responseBody);
        }

        @Test
        @DisplayName("실패 : 1차 신청(selectSectorId에 해당하는 구간을 찾을 수 없는 경우)")
        void fail3() throws Exception {
            // given
            String email = "admin@jnu.ac.kr";
            HashResult result = captchaHashProcessor.hash(1L);
            String captchaAnswer = "1234";

            String accessToken = jwtGenerator.generateAccessToken(email, "ADMIN");

            FinalSaveRequest request =
                    FinalSaveRequest.builder()
                            .captchaCode(result.getCaptchaCode())
                            .captchaAnswer(captchaAnswer)
                            .name("박영규")
                            .affiliation("AI융합대")
                            .studentNum("215551")
                            .carNum("12나1234")
                            .isLight(true)
                            .phoneNum("010-1111-2222")
                            .selectSectorId(100L)
                            .build();
            String requestBody = om.writeValueAsString(request);

            // when
            ResultActions resultActions =
                    mvc.perform(
                            post("/v1/registration/1")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .header("Authorization", "Bearer " + accessToken)
                                    .content(requestBody));

            // eye
            String responseBody = resultActions.andReturn().getResponse().getContentAsString();

            // then
            resultActions.andExpectAll(
                    status().isNotFound(),
                    jsonPath("$.reason").value(SectorErrorCode.NOT_FOUND_SECTOR.getReason()),
                    jsonPath("$.code").value(SectorErrorCode.NOT_FOUND_SECTOR.getCode()));
            resultActions.andDo(MockMvcResultHandlers.print()).andDo(document);
            log.info("responseBody : {}", responseBody);
        }

        @Test
        @DisplayName("실패 : 1차 신청(이름이 null 혹은 공백인 경우)")
        void fail5() throws Exception {
            // given
            String email = "admin@jnu.ac.kr";
            HashResult result = captchaHashProcessor.hash(1L);
            String captchaAnswer = "1234";
            String target = "이름을 ";

            String accessToken = jwtGenerator.generateAccessToken(email, "ADMIN");

            FinalSaveRequest request =
                    FinalSaveRequest.builder()
                            .captchaCode(result.getCaptchaCode())
                            .captchaAnswer(captchaAnswer)
                            .name(" ")
                            .affiliation("AI융합대")
                            .studentNum("215551")
                            .carNum("12나1234")
                            .isLight(true)
                            .phoneNum("010-1111-2222")
                            .selectSectorId(3L)
                            .build();
            String requestBody = om.writeValueAsString(request);

            // when
            ResultActions resultActions =
                    mvc.perform(
                            post("/v1/registration/1")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .header("Authorization", "Bearer " + accessToken)
                                    .content(requestBody));

            // eye
            String responseBody = resultActions.andReturn().getResponse().getContentAsString();

            // then
            resultActions.andExpectAll(
                    status().is4xxClientError(),
                    jsonPath("$.reason").value(target + ValidationMessage.MUST_NOT_BLANK),
                    jsonPath("$.code").value("BAD_REQUEST"));
            resultActions.andDo(MockMvcResultHandlers.print()).andDo(document);
            log.info("responseBody : {}", responseBody);
        }

        @Test
        @DisplayName("실패 : 1차 신청(소속대학이 null 혹은 공백인 경우)")
        void fail6() throws Exception {
            // given
            String email = "admin@jnu.ac.kr";
            HashResult result = captchaHashProcessor.hash(1L);
            String captchaAnswer = "1234";
            String target = "소속대학을 ";

            String accessToken = jwtGenerator.generateAccessToken(email, "ADMIN");

            FinalSaveRequest request =
                    FinalSaveRequest.builder()
                            .captchaCode(result.getCaptchaCode())
                            .captchaAnswer(captchaAnswer)
                            .name("박영규")
                            .affiliation(" ")
                            .studentNum("215551")
                            .carNum("12나1234")
                            .isLight(true)
                            .phoneNum("010-1111-2222")
                            .selectSectorId(3L)
                            .build();
            String requestBody = om.writeValueAsString(request);

            // when
            ResultActions resultActions =
                    mvc.perform(
                            post("/v1/registration/1")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .header("Authorization", "Bearer " + accessToken)
                                    .content(requestBody));

            // eye
            String responseBody = resultActions.andReturn().getResponse().getContentAsString();

            // then
            resultActions.andExpectAll(
                    status().is4xxClientError(),
                    jsonPath("$.reason").value(target + ValidationMessage.MUST_NOT_BLANK),
                    jsonPath("$.code").value("BAD_REQUEST"));
            resultActions.andDo(MockMvcResultHandlers.print()).andDo(document);
            log.info("responseBody : {}", responseBody);
        }

        @Test
        @DisplayName("실패 : 1차 신청(학번이 null 혹은 공백인 경우)")
        void fail7() throws Exception {
            // given
            String email = "admin@jnu.ac.kr";
            HashResult result = captchaHashProcessor.hash(1L);
            String captchaAnswer = "1234";
            String target = "학번을 ";

            String accessToken = jwtGenerator.generateAccessToken(email, "ADMIN");

            FinalSaveRequest request =
                    FinalSaveRequest.builder()
                            .captchaCode(result.getCaptchaCode())
                            .captchaAnswer(captchaAnswer)
                            .name("박영규")
                            .affiliation("AI융합대")
                            .studentNum(" ")
                            .carNum("12나1234")
                            .isLight(true)
                            .phoneNum("010-1111-2222")
                            .selectSectorId(3L)
                            .build();
            String requestBody = om.writeValueAsString(request);

            // when
            ResultActions resultActions =
                    mvc.perform(
                            post("/v1/registration/1")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .header("Authorization", "Bearer " + accessToken)
                                    .content(requestBody));

            // eye
            String responseBody = resultActions.andReturn().getResponse().getContentAsString();

            // then
            resultActions.andExpectAll(
                    status().is4xxClientError(),
                    jsonPath("$.reason").value(target + ValidationMessage.MUST_NOT_BLANK),
                    jsonPath("$.code").value("BAD_REQUEST"));
            resultActions.andDo(MockMvcResultHandlers.print()).andDo(document);
            log.info("responseBody : {}", responseBody);
        }

        @Test
        @DisplayName("실패 : 1차 신청(차량번호가 null 혹은 공백인 경우)")
        void fail8() throws Exception {
            // given
            String email = "admin@jnu.ac.kr";
            HashResult result = captchaHashProcessor.hash(1L);
            String captchaAnswer = "1234";
            String target = "차량번호를 ";

            String accessToken = jwtGenerator.generateAccessToken(email, "ADMIN");

            FinalSaveRequest request =
                    FinalSaveRequest.builder()
                            .captchaCode(result.getCaptchaCode())
                            .captchaAnswer(captchaAnswer)
                            .name("박영규")
                            .affiliation("AI융합대")
                            .studentNum("215551")
                            .carNum(" ")
                            .isLight(true)
                            .phoneNum("010-1111-2222")
                            .selectSectorId(3L)
                            .build();
            String requestBody = om.writeValueAsString(request);

            // when
            ResultActions resultActions =
                    mvc.perform(
                            post("/v1/registration/1")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .header("Authorization", "Bearer " + accessToken)
                                    .content(requestBody));

            // eye
            String responseBody = resultActions.andReturn().getResponse().getContentAsString();

            // then
            resultActions.andExpectAll(
                    status().is4xxClientError(),
                    jsonPath("$.reason").value(target + ValidationMessage.MUST_NOT_BLANK),
                    jsonPath("$.code").value("BAD_REQUEST"));
            resultActions.andDo(MockMvcResultHandlers.print()).andDo(document);
            log.info("responseBody : {}", responseBody);
        }

        @Test
        @DisplayName("실패 : 1차 신청(선택 구간 Id가 양수가 아닌경우)")
        void fail9() throws Exception {
            // given
            String email = "admin@jnu.ac.kr";
            HashResult result = captchaHashProcessor.hash(1L);
            String captchaAnswer = "1234";
            String target = "구간 ID는 ";

            String accessToken = jwtGenerator.generateAccessToken(email, "ADMIN");

            FinalSaveRequest request =
                    FinalSaveRequest.builder()
                            .captchaCode(result.getCaptchaCode())
                            .captchaAnswer(captchaAnswer)
                            .name("박영규")
                            .affiliation("AI융합대")
                            .studentNum("215551")
                            .carNum("12나1234")
                            .isLight(true)
                            .phoneNum("010-1111-2222")
                            .selectSectorId(-2L)
                            .build();
            String requestBody = om.writeValueAsString(request);

            // when
            ResultActions resultActions =
                    mvc.perform(
                            post("/v1/registration/1")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .header("Authorization", "Bearer " + accessToken)
                                    .content(requestBody));

            // eye
            String responseBody = resultActions.andReturn().getResponse().getContentAsString();

            // then
            resultActions.andExpectAll(
                    status().is4xxClientError(),
                    jsonPath("$.reason").value(target + ValidationMessage.MUST_POSITIVE_NUMBER),
                    jsonPath("$.code").value("BAD_REQUEST"));
            resultActions.andDo(MockMvcResultHandlers.print()).andDo(document);
            log.info("responseBody : {}", responseBody);
        }

        @Test
        @DisplayName("실패 : 1차 신청(전화번호가 전화번호 형식이 아닌경우)")
        void fail10() throws Exception {
            // given
            String email = "admin@jnu.ac.kr";
            HashResult result = captchaHashProcessor.hash(1L);
            String captchaAnswer = "1234";

            String accessToken = jwtGenerator.generateAccessToken(email, "ADMIN");

            FinalSaveRequest request =
                    FinalSaveRequest.builder()
                            .captchaCode(result.getCaptchaCode())
                            .captchaAnswer(captchaAnswer)
                            .name("박영규")
                            .affiliation("AI융합대")
                            .studentNum("215551")
                            .carNum("12나1234")
                            .isLight(true)
                            .phoneNum("010")
                            .selectSectorId(3L)
                            .build();
            String requestBody = om.writeValueAsString(request);

            // when
            ResultActions resultActions =
                    mvc.perform(
                            post("/v1/registration/1")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .header("Authorization", "Bearer " + accessToken)
                                    .content(requestBody));

            // eye
            String responseBody = resultActions.andReturn().getResponse().getContentAsString();

            // then
            resultActions.andExpectAll(
                    status().is4xxClientError(),
                    jsonPath("$.reason").value(ValidationMessage.IS_NOT_VALID_PHONE),
                    jsonPath("$.code").value("BAD_REQUEST"));
            resultActions.andDo(MockMvcResultHandlers.print()).andDo(document);
            log.info("responseBody : {}", responseBody);
        }

        @Test
        @DisplayName("실패 : 1차 신청(경차 여부가 null인 경우)")
        void fail11() throws Exception {
            // given
            String email = "admin@jnu.ac.kr";
            HashResult result = captchaHashProcessor.hash(1L);
            String captchaAnswer = "1234";
            String target = "경차 여부를 ";

            String accessToken = jwtGenerator.generateAccessToken(email, "ADMIN");

            FinalSaveRequest request =
                    FinalSaveRequest.builder()
                            .captchaCode(result.getCaptchaCode())
                            .captchaAnswer(captchaAnswer)
                            .name("박영규")
                            .affiliation("AI융합대")
                            .studentNum("215551")
                            .carNum("12나1234")
                            .isLight(null)
                            .phoneNum("010")
                            .selectSectorId(3L)
                            .build();
            String requestBody = om.writeValueAsString(request);

            // when
            ResultActions resultActions =
                    mvc.perform(
                            post("/v1/registration/1")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .header("Authorization", "Bearer " + accessToken)
                                    .content(requestBody));

            // eye
            String responseBody = resultActions.andReturn().getResponse().getContentAsString();

            // then
            resultActions.andExpectAll(
                    status().is4xxClientError(),
                    jsonPath("$.reason").value(target + ValidationMessage.MUST_NOT_NULL),
                    jsonPath("$.code").value("BAD_REQUEST"));
            resultActions.andDo(MockMvcResultHandlers.print()).andDo(document);
            log.info("responseBody : {}", responseBody);
        }

        @Test
        @DisplayName("실패 : 1차 신청(캡챠 코드가 null 혹은 공백인 경우)")
        void fail13() throws Exception {
            // given
            String email = "admin@jnu.ac.kr";
            String captchaAnswer = "1234";
            String target = "캡챠 코드를 ";

            String accessToken = jwtGenerator.generateAccessToken(email, "ADMIN");

            FinalSaveRequest request =
                    FinalSaveRequest.builder()
                            .captchaCode(null)
                            .captchaAnswer(captchaAnswer)
                            .name("박영규")
                            .affiliation("AI융합대")
                            .studentNum("215551")
                            .carNum("12나1234")
                            .isLight(true)
                            .phoneNum("010")
                            .selectSectorId(3L)
                            .build();
            String requestBody = om.writeValueAsString(request);

            // when
            ResultActions resultActions =
                    mvc.perform(
                            post("/v1/registration/1")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .header("Authorization", "Bearer " + accessToken)
                                    .content(requestBody));

            // eye
            String responseBody = resultActions.andReturn().getResponse().getContentAsString();

            // then
            resultActions.andExpectAll(
                    status().is4xxClientError(),
                    jsonPath("$.reason").value(target + ValidationMessage.MUST_NOT_BLANK),
                    jsonPath("$.code").value("BAD_REQUEST"));
            resultActions.andDo(MockMvcResultHandlers.print()).andDo(document);
            log.info("responseBody : {}", responseBody);
        }

        @Test
        @DisplayName("실패 : 1차 신청(캡챠 답변이 null 혹은 공백인 경우)")
        void fail14() throws Exception {
            // given
            String email = "admin@jnu.ac.kr";
            HashResult result = captchaHashProcessor.hash(1L);
            String target = "캡챠 답변을 ";

            String accessToken = jwtGenerator.generateAccessToken(email, "ADMIN");

            FinalSaveRequest request =
                    FinalSaveRequest.builder()
                            .captchaCode(result.getCaptchaCode())
                            .captchaAnswer(null)
                            .name("박영규")
                            .affiliation("AI융합대")
                            .studentNum("215551")
                            .carNum("12나1234")
                            .isLight(true)
                            .phoneNum("010")
                            .selectSectorId(3L)
                            .build();
            String requestBody = om.writeValueAsString(request);

            // when
            ResultActions resultActions =
                    mvc.perform(
                            post("/v1/registration/1")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .header("Authorization", "Bearer " + accessToken)
                                    .content(requestBody));

            // eye
            String responseBody = resultActions.andReturn().getResponse().getContentAsString();

            // then
            resultActions.andExpectAll(
                    status().is4xxClientError(),
                    jsonPath("$.reason").value(target + ValidationMessage.MUST_NOT_BLANK),
                    jsonPath("$.code").value("BAD_REQUEST"));
            resultActions.andDo(MockMvcResultHandlers.print()).andDo(document);
            log.info("responseBody : {}", responseBody);
        }
    }
}
