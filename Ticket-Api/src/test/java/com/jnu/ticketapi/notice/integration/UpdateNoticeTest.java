package com.jnu.ticketapi.notice.integration;

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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@AutoConfigureMockMvc
@ExtendWith(DatabaseClearExtension.class)
public class UpdateNoticeTest {

    @Autowired private MockMvc mvc;

    @Autowired private ObjectMapper om;

    @Test
    @DisplayName("성공 : 안내사항 수정")
    @WithMockUser(roles = "COUNCIL")
    public void update_notice_test() throws Exception {

        // given
        UpdateNoticeRequest requestDto =
                UpdateNoticeRequest.builder().noticeContent("업데이트할 내용").build();
        String requestBody = om.writeValueAsString(requestDto);

        // when
        ResultActions resultActions =
                mvc.perform(
                        put("/v1/notice")
                                .contentType(MediaType.APPLICATION_JSON)
                                .characterEncoding(StandardCharsets.UTF_8)
                                .content(requestBody));

        // eye
        String responseBody =
                resultActions.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        log.info("responseBody : " + responseBody);
        // then
        resultActions.andExpectAll(status().isOk(), jsonPath("$.success").value(true));
    }
}
