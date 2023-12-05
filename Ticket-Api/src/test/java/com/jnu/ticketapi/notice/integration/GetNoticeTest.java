package com.jnu.ticketapi.notice.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jnu.ticketapi.Announce.config.DatabaseClearExtension;
import com.jnu.ticketapi.api.notice.model.request.UpdateNoticeRequest;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@AutoConfigureMockMvc
@ExtendWith(DatabaseClearExtension.class)
public class GetNoticeTest {

    @Autowired private MockMvc mvc;

    @Autowired private ObjectMapper om;

    @Test
    @DisplayName("성공 : 안내사항 조회")
    @WithMockUser(roles = "COUNCIL")
    void get_notice_test() throws Exception {
        // give
        String noticeContent = "테스트 안내사항";
        UpdateNoticeRequest updateNoticeRequest =
                UpdateNoticeRequest.builder().noticeContent(noticeContent).build();
        String givenRequestBody = om.writeValueAsString(updateNoticeRequest);

        ResultActions givenRequest =
                mvc.perform(
                        put("/v1/notice")
                                .contentType(MediaType.APPLICATION_JSON)
                                .characterEncoding(StandardCharsets.UTF_8)
                                .content(givenRequestBody));
        String givenResponse =
                givenRequest.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        log.info("givenResponse : " + givenResponse);

        // when
        ResultActions resultActions =
                mvc.perform(
                        get("/v1/notice")
                                .contentType(MediaType.APPLICATION_JSON)
                                .characterEncoding(StandardCharsets.UTF_8));

        // eye
        String responseBody =
                resultActions.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        log.info("responseBody : " + responseBody);

        // then
        resultActions.andExpectAll(
                status().isOk(),
                jsonPath("$.success").value(true),
                jsonPath("$.data.noticeContent").value(noticeContent));
    }

    @Test
    @DisplayName("실패 : 안내사항 조회(안내사항이 존재하지 않는 경우 404)")
    @WithAnonymousUser
    void get_notice_fail_test() throws Exception {
        // give

        // when
        ResultActions resultActions =
                mvc.perform(
                        get("/v1/notice")
                                .contentType(MediaType.APPLICATION_JSON)
                                .characterEncoding(StandardCharsets.UTF_8));

        // eye
        String responseBody =
                resultActions.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        log.info("responseBody : " + responseBody);

        // then
        resultActions.andExpectAll(
                status().is4xxClientError(),
                jsonPath("$.success").value(false),
                jsonPath("$.code").value("NOTICE_404_1"));
    }
}
