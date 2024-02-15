package com.jnu.ticketapi.registration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jnu.ticketapi.RestDocsConfig;
import com.jnu.ticketapi.security.JwtGenerator;
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
public class GetRegistrationTest extends RestDocsConfig {
    @Autowired private MockMvc mvc;

    @Autowired private ObjectMapper om;

    @Autowired JwtGenerator jwtGenerator;

    @Nested
    class getRegistrationTest {

        @Test
        @DisplayName("성공 : 임시저장 조회(임시저장을 안했을 경우)")
        void success() throws Exception {
            // given
            String email = "user4@jnu.ac.kr";
            String accessToken = jwtGenerator.generateAccessToken(email, "ADMIN");

            // when
            ResultActions resultActions =
                    mvc.perform(
                            get("/v1/registration")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .header("Authorization", "Bearer " + accessToken));
            // eye
            String responseBody = resultActions.andReturn().getResponse().getContentAsString();

            // then
            resultActions.andExpectAll(
                    status().isOk(),
                    jsonPath("$.email").value(email),
                    jsonPath("$.studentNum").isEmpty(),
                    jsonPath("$.phoneNum").isEmpty());
            resultActions.andDo(MockMvcResultHandlers.print()).andDo(document);
            log.info("responseBody : {}", responseBody);
        }

        @Test
        @DisplayName("성공 : 임시저장 조회(임시저장을 했을 경우)")
        void success2() throws Exception {
            // given
            String email = "user@jnu.ac.kr";
            String accessToken = jwtGenerator.generateAccessToken(email, "USER");

            // when
            ResultActions resultActions =
                    mvc.perform(
                            get("/v1/registration")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .header("Authorization", "Bearer " + accessToken));
            // eye
            String responseBody = resultActions.andReturn().getResponse().getContentAsString();

            // then
            resultActions.andExpectAll(
                    status().isOk(),
                    jsonPath("$.email").value(email),
                    jsonPath("$.name").value("이진혁"),
                    jsonPath("$.studentNum").value("215555"));
            resultActions.andDo(MockMvcResultHandlers.print()).andDo(document);
            log.info("responseBody : {}", responseBody);
        }
    }
}
