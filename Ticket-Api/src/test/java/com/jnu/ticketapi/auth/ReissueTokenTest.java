package com.jnu.ticketapi.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jnu.ticketapi.RestDocsConfig;
import com.jnu.ticketapi.api.auth.model.request.LoginUserRequest;
import com.jnu.ticketapi.api.auth.model.request.ReissueTokenRequest;
import com.jnu.ticketapi.config.DatabaseClearExtension;
import com.jnu.ticketapi.security.JwtGenerator;
import com.jnu.ticketapi.security.JwtResolver;
import com.jnu.ticketcommon.consts.TicketStatic;
import com.jnu.ticketcommon.exception.GlobalErrorCode;
import com.jnu.ticketcommon.message.ValidationMessage;
import com.jnu.ticketinfrastructure.redis.RedisService;
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
public class ReissueTokenTest extends RestDocsConfig {
    @Autowired private MockMvc mvc;

    @Autowired private ObjectMapper om;

    @Autowired private JwtGenerator generator;

    @Autowired private JwtResolver resolver;

    @Autowired private RedisService redisService;

    private final String email = "ekrrrdj21@jnu.ac.kr";
    private final String pwd = "Dlwlsgur@123";

    @BeforeEach
    void setUp() throws Exception {
        // given
        LoginUserRequest requestsDto = LoginUserRequest.builder().email(email).pwd(pwd).build();
        String requestBody = om.writeValueAsString(requestsDto);
        // when
        ResultActions resultActions =
                mvc.perform(
                        post("/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody));
    }

    @Nested
    @DisplayName("토큰 재발급 테스트")
    class reissueTokenTest {
        @Test
        @DisplayName("성공 : 토큰 재발급")
        void success() throws Exception {
            // given
            String accessToken = generator.generateAccessToken(email, "USER");
            /*
            redis에 저장된 refreshToken 가져오기
             */
            String refreshToken =
                    redisService.getValues("RT(" + TicketStatic.SERVER + "):" + email);
            ReissueTokenRequest request =
                    ReissueTokenRequest.builder().refreshToken(refreshToken).build();
            String requestBody = om.writeValueAsString(request);

            // when
            ResultActions resultActions =
                    mvc.perform(
                            post("/v1/auth/reissue")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .header("Authorization", "Bearer " + accessToken)
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
        @DisplayName("실패 : 토큰 재발급(자신의 리프레시토큰이 아닐경우)")
        void fail() throws Exception {
            // given
            String accessToken = generator.generateAccessToken(email, "USER");
            String fakeEmail = "i'm faker";
            /*
            redis에 저장된 refreshToken 가져오기
             */
            String refreshToken = generator.generateRefreshToken(fakeEmail, "USER");
            ReissueTokenRequest request =
                    ReissueTokenRequest.builder().refreshToken(refreshToken).build();
            String requestBody = om.writeValueAsString(request);

            // when
            ResultActions resultActions =
                    mvc.perform(
                            post("/v1/auth/reissue")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .header("Authorization", "Bearer " + accessToken)
                                    .content(requestBody));

            // eye
            String responseBody = resultActions.andReturn().getResponse().getContentAsString();

            // then
            resultActions.andExpectAll(
                    status().isForbidden(),
                    jsonPath("$.reason").value(GlobalErrorCode.NOT_EQUAL_PRINCIPAL.getReason()),
                    jsonPath("$.status").value(403));
            resultActions.andDo(MockMvcResultHandlers.print()).andDo(document);
            log.info("responseBody : " + responseBody);
        }

        @Test
        @DisplayName("실패 : 토큰 재발급(리프레시 토큰의 인증시간이 만료되었을 경)")
        void fail2() throws Exception {
            // given
            String accessToken = generator.generateAccessToken(email, "USER");
            /*
            redis에 저장된 refreshToken 가져오기
             */
            String refreshToken =
                    "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJFbWFpbCI6ImVrcnJyZGoyMUBqbnUuYWMua3IiLCJBdXRoIjoiVVNFUiIsImlhdCI6MTcwMjM5Mzg5NiwiZXhwIjoxNzAyMzk1Njk2fQ.oFQN-YuTTrQxtg3lGKyHv5AWlTQSzRKHPduyia1B3o4";
            ReissueTokenRequest request =
                    ReissueTokenRequest.builder().refreshToken(refreshToken).build();
            String requestBody = om.writeValueAsString(request);

            // when
            ResultActions resultActions =
                    mvc.perform(
                            post("/v1/auth/reissue")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .header("Authorization", "Bearer " + accessToken)
                                    .content(requestBody));

            // eye
            String responseBody = resultActions.andReturn().getResponse().getContentAsString();

            // then
            resultActions.andExpectAll(
                    status().isForbidden(),
                    jsonPath("$.reason").value(GlobalErrorCode.REFRESH_TOKEN_EXPIRED.getReason()),
                    jsonPath("$.status").value(403));
            resultActions.andDo(MockMvcResultHandlers.print()).andDo(document);
            log.info("responseBody : " + responseBody);
        }

        @Test
        @DisplayName("실패 : 토큰 재발급(리프레시 토큰이 유효하지 않은 경우")
        void fail3() throws Exception {
            // given
            String accessToken = generator.generateAccessToken(email, "USER");
            /*
            redis에 저장된 refreshToken 가져오기
             */
            String refreshToken = "I'm Faker";
            ReissueTokenRequest request =
                    ReissueTokenRequest.builder().refreshToken(refreshToken).build();
            String requestBody = om.writeValueAsString(request);

            // when
            ResultActions resultActions =
                    mvc.perform(
                            post("/v1/auth/reissue")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .header("Authorization", "Bearer " + accessToken)
                                    .content(requestBody));

            // eye
            String responseBody = resultActions.andReturn().getResponse().getContentAsString();

            // then
            resultActions.andExpectAll(
                    status().isForbidden(),
                    jsonPath("$.reason").value(GlobalErrorCode.REFRESH_TOKEN_NOT_VALID.getReason()),
                    jsonPath("$.status").value(403));
            resultActions.andDo(MockMvcResultHandlers.print()).andDo(document);
            log.info("responseBody : " + responseBody);
        }

        @Test
        @DisplayName("실패 : 토큰 재발급(리프레시 토큰이 null 혹은 빈 값으로 들어올 경우")
        void fail4() throws Exception {
            // given
            String accessToken = generator.generateAccessToken(email, "USER");
            /*
            redis에 저장된 refreshToken 가져오기
             */
            String refreshToken = " ";
            ReissueTokenRequest request =
                    ReissueTokenRequest.builder().refreshToken(refreshToken).build();
            String requestBody = om.writeValueAsString(request);

            // when
            ResultActions resultActions =
                    mvc.perform(
                            post("/v1/auth/reissue")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .header("Authorization", "Bearer " + accessToken)
                                    .content(requestBody));

            // eye
            String responseBody = resultActions.andReturn().getResponse().getContentAsString();

            // then
            resultActions.andExpectAll(
                    status().is4xxClientError(),
                    jsonPath("$.reason").value("리프레시 토큰을 " + ValidationMessage.MUST_NOT_BLANK),
                    jsonPath("$.status").value(400));
            resultActions.andDo(MockMvcResultHandlers.print()).andDo(document);
            log.info("responseBody : " + responseBody);
        }
    }
}
