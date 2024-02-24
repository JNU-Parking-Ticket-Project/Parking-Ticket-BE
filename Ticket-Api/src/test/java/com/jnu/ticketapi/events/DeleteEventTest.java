package com.jnu.ticketapi.events;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jnu.ticketapi.RestDocsConfig;
import com.jnu.ticketapi.security.JwtGenerator;
import com.jnu.ticketcommon.message.ResponseMessage;
import com.jnu.ticketdomain.domains.events.exception.EventErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
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
@WithMockUser(roles = "ADMIN")
@Sql("classpath:db/teardown.sql")
public class DeleteEventTest extends RestDocsConfig {
    @Autowired private MockMvc mvc;

    @Autowired JwtGenerator jwtGenerator;

    @Autowired private ObjectMapper om;

    @Nested
    class deleteEventTest {
        @Test
        @DisplayName("성공 : 이벤트 삭제")
        void success() throws Exception {
            // given
            String email = "admin@jnu.ac.kr";
            Long eventId = 1L;
            String accessToken = jwtGenerator.generateAccessToken(email, "ADMIN");

            // when
            ResultActions resultActions =
                    mvc.perform(
                            delete("/v1/events/" + eventId)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .header("Authorization", "Bearer " + accessToken));

            // eye
            String responseBody = resultActions.andReturn().getResponse().getContentAsString();

            // then
            resultActions.andExpectAll(
                    status().isOk(),
                    jsonPath("$.message").value(ResponseMessage.EVENT_SUCCESS_DELETE_MESSAGE));

            resultActions.andDo(MockMvcResultHandlers.print()).andDo(document);
            log.info("responseBody : {}", responseBody);
        }

        @Test
        @DisplayName("실패 : 이벤트 삭제 - 존재하지 않는 이벤트")
        void fail_not_found_event() throws Exception {
            // given
            String email = "admin@jnu.ac.kr";
            Long eventId = 100L;
            String accessToken = jwtGenerator.generateAccessToken(email, "ADMIN");

            // when
            ResultActions resultActions =
                    mvc.perform(
                            delete("/v1/events/" + eventId)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .header("Authorization", "Bearer " + accessToken));

            // eye
            String responseBody = resultActions.andReturn().getResponse().getContentAsString();

            // then
            resultActions.andExpectAll(
                    status().isNotFound(),
                    jsonPath("$.reason").value(EventErrorCode.NOT_FOUND_EVENT.getReason()));

            resultActions.andDo(MockMvcResultHandlers.print()).andDo(document);
            log.info("responseBody : {}", responseBody);
        }
    }
}
