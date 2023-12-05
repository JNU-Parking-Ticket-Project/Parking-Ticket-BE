package com.jnu.ticketapi.Announce.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jnu.ticketapi.api.announce.model.request.SaveAnnounceRequest;
import com.jnu.ticketapi.api.announce.model.request.UpdateAnnounceRequest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.nio.charset.StandardCharsets;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class UpdateAnnounceTest {
    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper om;

    @Test
    @DisplayName("성공 : 공지사항 수정")
    @WithMockUser(roles = "COUNCIL")
    void save_announces_test() throws Exception {
        {
            // given
            SaveAnnounceRequest saveRequest = SaveAnnounceRequest.builder()
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
            String testResponseBody = testResultActions.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
            log.info("responseBody : " + testResponseBody);

            UpdateAnnounceRequest request = UpdateAnnounceRequest.builder()
                    .announceTitle("수정된 제목")
                    .announceContent("수정된 내용")
                    .build();
            String requestBody = om.writeValueAsString(request);
            long announceId = 1L;



            // when
            ResultActions resultActions =
                    mvc.perform(
                            put("/v1/announce/" + announceId)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .characterEncoding(StandardCharsets.UTF_8)
                                    .content(requestBody)
                    );

            // eye
            String responseBody = resultActions.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
            log.info("responseBody : " + responseBody);
            // then
            resultActions.andExpectAll(
                    status().isOk(),
                    jsonPath("$.success").value(true));

        }
    }
}
