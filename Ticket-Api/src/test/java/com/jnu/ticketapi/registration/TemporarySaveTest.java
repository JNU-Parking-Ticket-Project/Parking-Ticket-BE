package com.jnu.ticketapi.registration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jnu.ticketapi.RestDocsConfig;
import com.jnu.ticketapi.api.registration.model.request.TemporarySaveRequest;
import com.jnu.ticketapi.security.JwtGenerator;
import com.jnu.ticketcommon.message.ValidationMessage;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@AutoConfigureMockMvc
@AutoConfigureRestDocs
@Sql("classpath:db/teardown.sql")
public class TemporarySaveTest extends RestDocsConfig {
    @Autowired private MockMvc mvc;

    @Autowired private ObjectMapper om;

    @Autowired JwtGenerator jwtGenerator;

    @Nested
    class temporarySaveTest {
        @Test
        @DisplayName("성공 : 임시저장")
        void success() throws Exception {
            // given
            String email = "admin@jnu.ac.kr";
            String accessToken = jwtGenerator.generateAccessToken(email, "ADMIN");

            TemporarySaveRequest request =
                    TemporarySaveRequest.builder()
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
                            post("/v1/registration/temporary")
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

        @Test
        @DisplayName("실패 : 임시저장(요청하는 사용자를 찾을 수 없는 경우)")
        void fail() throws Exception {
            // given
            String email = "imFaker@T1.com";
            String accessToken = jwtGenerator.generateAccessToken(email, "USER");

            TemporarySaveRequest request =
                    TemporarySaveRequest.builder()
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
                            post("/v1/registration/temporary")
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
        @DisplayName("실패 : 임시저장(selectSectorId에 해당하는 구간을 찾을 수 없는 경우)")
        void fail2() throws Exception {
            // given
            String email = "admin@jnu.ac.kr";
            String accessToken = jwtGenerator.generateAccessToken(email, "ADMIN");

            TemporarySaveRequest request =
                    TemporarySaveRequest.builder()
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
                            post("/v1/registration/temporary")
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
        @DisplayName("실패 : 임시저장(이름이 null 혹은 공백인 경우)")
        void fail3() throws Exception {
            // given
            String email = "admin@jnu.ac.kr";
            String accessToken = jwtGenerator.generateAccessToken(email, "ADMIN");
            String target = "이름을 ";

            TemporarySaveRequest request =
                    TemporarySaveRequest.builder()
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
                            post("/v1/registration/temporary")
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
        @DisplayName("실패 : 임시저장(소속대학이 null 혹은 공백인 경우)")
        void fail4() throws Exception {
            // given
            String email = "admin@jnu.ac.kr";
            String accessToken = jwtGenerator.generateAccessToken(email, "ADMIN");
            String target = "소속대학을 ";

            TemporarySaveRequest request =
                    TemporarySaveRequest.builder()
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
                            post("/v1/registration/temporary")
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
        @DisplayName("실패 : 임시저장(학번이 null 혹은 공백인 경우)")
        void fail5() throws Exception {
            // given
            String email = "admin@jnu.ac.kr";
            String accessToken = jwtGenerator.generateAccessToken(email, "ADMIN");
            String target = "학번을 ";

            TemporarySaveRequest request =
                    TemporarySaveRequest.builder()
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
                            post("/v1/registration/temporary")
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
        @DisplayName("실패 : 임시저장(차량번호가 null 혹은 공백인 경우)")
        void fail6() throws Exception {
            // given
            String email = "admin@jnu.ac.kr";
            String accessToken = jwtGenerator.generateAccessToken(email, "ADMIN");
            String target = "차량번호를 ";

            TemporarySaveRequest request =
                    TemporarySaveRequest.builder()
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
                            post("/v1/registration/temporary")
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
        @DisplayName("실패 : 임시저장(선택 구간 Id가 양수가 아닌경우)")
        void fail7() throws Exception {
            // given
            String email = "admin@jnu.ac.kr";
            String accessToken = jwtGenerator.generateAccessToken(email, "ADMIN");
            String target = "구간 ID는 ";

            TemporarySaveRequest request =
                    TemporarySaveRequest.builder()
                            .name("박영규")
                            .affiliation("AI융합대")
                            .studentNum("215551")
                            .carNum("12나1234")
                            .isLight(true)
                            .phoneNum("010-000-0000")
                            .selectSectorId(-2L)
                            .build();
            String requestBody = om.writeValueAsString(request);

            // when
            ResultActions resultActions =
                    mvc.perform(
                            post("/v1/registration/temporary")
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
        @DisplayName("실패 : 임시저장(전화번호가 전화번호 형식이 아닌경우)")
        void fail8() throws Exception {
            // given
            String email = "admin@jnu.ac.kr";
            String accessToken = jwtGenerator.generateAccessToken(email, "ADMIN");

            TemporarySaveRequest request =
                    TemporarySaveRequest.builder()
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
                            post("/v1/registration/temporary")
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
        @DisplayName("실패 : 임시저장(경차 여부가 null인 경우)")
        @Deprecated
        // 경차 여부는 default가 false이기 때문에 request로 null 혹은 공백이 와도 에러가 터지지않음.
        void fail9() throws Exception {
            // given
            String email = "admin@jnu.ac.kr";
            String accessToken = jwtGenerator.generateAccessToken(email, "ADMIN");
            String target = "경차 여부를 ";

            TemporarySaveRequest request =
                    TemporarySaveRequest.builder()
                            .name("박영규")
                            .affiliation("AI융합대")
                            .studentNum("215551")
                            .carNum("12나1234")
                            .isLight(null)
                            .phoneNum("010-000-0000")
                            .selectSectorId(3L)
                            .build();
            log.info("request : {}", request);
            String requestBody = om.writeValueAsString(request);

            // when
            ResultActions resultActions =
                    mvc.perform(
                            post("/v1/registration/temporary")
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
    }
}
