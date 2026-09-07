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
public class GetRegistrationsTest extends RestDocsConfig {
    @Autowired private MockMvc mvc;
    @Autowired private JwtGenerator jwtGenerator;
    @Autowired private ObjectMapper om;

    @Nested
    class getRegistrationListTest {
        @Test
        @DisplayName("성공 : 신청 목록 조회")
        void success() throws Exception {
            // given
            Long eventId = 1L;
            String accessToken = jwtGenerator.generateAccessToken("council@jnu.ac.kr", "COUNCIL");
            // when
            ResultActions resultActions =
                    mvc.perform(
                            get("/v1/registrations/" + eventId)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .header("Authorization", "Bearer " + accessToken));

            // eye
            String responseBody = resultActions.andReturn().getResponse().getContentAsString();

            // then
            resultActions.andExpectAll(
                    status().isOk(),
                    jsonPath("$.registrations[0].email").value("council@jnu.ac.kr"),
                    jsonPath("$.registrations[0].position").value(1),
                    jsonPath("$.registrations[0].resultStatus").value("합격"),
                    jsonPath("$.registrations[0].sequence").value(-2),
                    jsonPath("$.registrations[0].savedAtEstimated").value(false));
            resultActions.andDo(MockMvcResultHandlers.print()).andDo(document);
            log.info("responseBody : {}", responseBody);
        }

        @Test
        @DisplayName("실패 : 일반 사용자는 신청 목록을 조회할 수 없다")
        void userCannotReadRegistrationList() throws Exception {
            String accessToken = jwtGenerator.generateAccessToken("user@jnu.ac.kr", "USER");

            mvc.perform(
                            get("/v1/registrations/1")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isForbidden());
        }
    }
}
