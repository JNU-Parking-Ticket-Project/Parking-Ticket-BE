package com.jnu.ticketapi.Announce.integration;

import com.jnu.ticketapi.Announce.config.DatabaseClearExtension;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@AutoConfigureMockMvc
@ExtendWith(DatabaseClearExtension.class)
public class GetAnnouncesTest {

    @Autowired private MockMvc mvc;

    @Test
    @DisplayName("성공 : 공지사항 목록 조회")
    @WithAnonymousUser
    void get_announces_test() throws Exception {
        {
            // given
            MultiValueMap<String, String> request = new LinkedMultiValueMap<>();
            request.add("page", "0");
            request.add("size", "10");
            request.add("sort", "createdAt,DESC");

            // when
            ResultActions resultActions =
                    mvc.perform(
                            get("/v1/announce")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .queryParams(request));
            // eye
            String responseBody = resultActions.andReturn().getResponse().getContentAsString();
            log.info("responseBody : " + responseBody);
            // then
            resultActions.andExpectAll(status().isOk(), jsonPath("$.success").value(true));
        }
    }
}
