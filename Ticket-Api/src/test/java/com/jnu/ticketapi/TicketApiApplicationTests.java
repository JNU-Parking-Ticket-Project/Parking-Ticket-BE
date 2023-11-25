package com.jnu.ticketapi;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jnu.ticketapi.dto.LoginUserRequestDto;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
// @Sql("classpath:db/teardown.sql")
@AutoConfigureMockMvc
class TicketApiApplicationTests {

    @Autowired private MockMvc mvc;

    @Autowired private ObjectMapper om;

    @Nested
    class loginTest {
        @Test
        @DisplayName("성공 : 로그인")
        void loginTest() throws Exception {
            {
                // given
                LoginUserRequestDto requestsDto =
                        LoginUserRequestDto.builder()
                                .email("ekrrrdj21@jnu.ac.kr")
                                .pwd("1234")
                                .build();
                String requestBody = om.writeValueAsString(requestsDto);
                // when
                ResultActions resultActions =
                        mvc.perform(
                                post("/v1/auth/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(requestBody));
                // eye
                String responseBody = resultActions.andReturn().getResponse().getContentAsString();
                // then
                resultActions.andExpectAll(
                        status().isOk(),
                        jsonPath("$.success").value(true),
                        /*
                        accessToken, refreshToken 요청마다 새로 발급되서 예측을 할 수 없어서 exists()로 검사
                         */
                        jsonPath("$.data.accessToken").exists());
                jsonPath("$.data.refreshToken").exists();
                log.info("responseBody : " + responseBody);
            }
        }
    }
}
