package com.jnu.ticketapi.Announce.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jnu.ticketapi.security.JwtGenerator;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Sql("classpath:db/teardown.sql")
public class DeleteAnnounceTest {
    @Autowired private MockMvc mvc;

    @Autowired private ObjectMapper om;

    @Autowired JwtGenerator jwtGenerator;

    @Test
    @DisplayName("성공 : 공지사항 삭제")
    @WithMockUser(roles = "COUNCIL")
    void delete_announces_test() throws Exception {
        {
            // given
            long announceId = 1L;
            String accessToken = jwtGenerator.generateAccessToken("admin@jnu.ac.kr", "ADMIN");

            // when
            ResultActions resultActions =
                    mvc.perform(
                            delete("/v1/announce/" + announceId)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .header("Authorization", "Bearer " + accessToken)
                                    .characterEncoding(StandardCharsets.UTF_8));

            // eye
            String responseBody =
                    resultActions
                            .andReturn()
                            .getResponse()
                            .getContentAsString(StandardCharsets.UTF_8);
            log.info("responseBody : " + responseBody);
            // then
            resultActions.andExpectAll(
                    status().isOk(), jsonPath("$.message").value("공지사항이 삭제 되었습니다."));
        }
    }

    @Test
    @DisplayName("실패 : 공지사항 목록 삭제(존재하지 않는 공지사항 ID 입력)")
    @WithMockUser(roles = "COUNCIL")
    void delete_announces_fail_test() throws Exception {
        {
            // given
            long announceId = 55L;
            String accessToken = jwtGenerator.generateAccessToken("admin@jnu.ac.kr", "ADMIN");

            // when
            ResultActions resultActions =
                    mvc.perform(
                            delete("/v1/announce/" + announceId)
                                    .header("Authorization", "Bearer " + accessToken)
                                    .contentType(MediaType.APPLICATION_JSON));
            // eye
            String responseBody =
                    resultActions
                            .andReturn()
                            .getResponse()
                            .getContentAsString(StandardCharsets.UTF_8);
            log.info("responseBody : " + responseBody);
            // then
            resultActions.andExpectAll(
                    status().is4xxClientError(), jsonPath("$.success").value(false));
        }
    }
}
