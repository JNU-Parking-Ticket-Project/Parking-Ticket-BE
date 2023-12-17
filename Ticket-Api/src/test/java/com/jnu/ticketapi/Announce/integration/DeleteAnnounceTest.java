package com.jnu.ticketapi.Announce.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jnu.ticketapi.api.announce.model.request.SaveAnnounceRequest;
import com.jnu.ticketapi.config.DatabaseClearExtension;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@AutoConfigureMockMvc
@ExtendWith(DatabaseClearExtension.class)
public class DeleteAnnounceTest {
    @Autowired private MockMvc mvc;

    @Autowired private ObjectMapper om;

    @Test
    @DisplayName("성공 : 공지사항 삭제")
    @WithMockUser(roles = "COUNCIL")
    void delete_announces_test() throws Exception {
        {
            // given
            SaveAnnounceRequest saveRequest =
                    SaveAnnounceRequest.builder()
                            .announceTitle("테스트 제목")
                            .announceContent("테스트 내용")
                            .build();
            String testRequestBody = om.writeValueAsString(saveRequest);

            ResultActions testResultActions =
                    mvc.perform(
                            post("/v1/announce")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .characterEncoding(StandardCharsets.UTF_8)
                                    .content(testRequestBody));
            String testResponseBody =
                    testResultActions
                            .andReturn()
                            .getResponse()
                            .getContentAsString(StandardCharsets.UTF_8);
            log.info("responseBody : " + testResponseBody);

            long announceId = 1L;

            // when
            ResultActions resultActions =
                    mvc.perform(
                            delete("/v1/announce/" + announceId)
                                    .contentType(MediaType.APPLICATION_JSON)
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

            // when
            ResultActions resultActions =
                    mvc.perform(
                            delete("/v1/announce/" + announceId)
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
