package com.jnu.ticketapi.Announce.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
public class SaveAnnounceTest {

    @Autowired private MockMvc mvc;

    @Autowired private ObjectMapper om;

    @Test
    @DisplayName("성공 : 공지사항 작성")
    @WithMockUser(roles = "COUNCIL")
    void save_announces_test() throws Exception {
        {
            // given
            SaveAnnounceRequest request =
                    SaveAnnounceRequest.builder()
                            .announceTitle("테스트 제목")
                            .announceContent("테스트 내용")
                            .build();
            String requestBody = om.writeValueAsString(request);

            // when
            ResultActions resultActions =
                    mvc.perform(
                            post("/v1/announce")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .characterEncoding(StandardCharsets.UTF_8)
                                    .content(requestBody));
            // eye
            String responseBody =
                    resultActions
                            .andReturn()
                            .getResponse()
                            .getContentAsString(StandardCharsets.UTF_8);
            log.info("responseBody : " + responseBody);
            // then
            resultActions.andExpectAll(
                    status().isOk(), jsonPath("$.announceTitle").value("테스트 제목"));
        }
    }

    @Test
    @DisplayName("성공 : 공지사항 작성(제목, 내용 Default Value 테스트)")
    @WithMockUser(roles = "COUNCIL")
    void save_announces_empty_test() throws Exception {
        {
            // given
            SaveAnnounceRequest request = SaveAnnounceRequest.builder().build();
            String requestBody = om.writeValueAsString(request);

            // when
            ResultActions resultActions =
                    mvc.perform(
                            post("/v1/announce")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .characterEncoding(StandardCharsets.UTF_8)
                                    .content(requestBody));
            // eye
            String responseBody =
                    resultActions
                            .andReturn()
                            .getResponse()
                            .getContentAsString(StandardCharsets.UTF_8);
            log.info("responseBody : " + responseBody);
            // then
            resultActions.andExpectAll(status().isOk());
        }
    }
}
