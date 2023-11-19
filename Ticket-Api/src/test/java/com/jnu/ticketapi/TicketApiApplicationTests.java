package com.jnu.ticketapi;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.jnu.ticketdomain.domains.dto.LoginUserRequestsDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;


@WithMockUser
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
//@Sql("classpath:db/teardown.sql")
@AutoConfigureMockMvc
class TicketApiApplicationTests {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper om;

    @Nested
    class loginTest {
        @Test
        @DisplayName("성공 : 로그인")
        void loginTest() throws Exception {
            {
                // given
                LoginUserRequestsDto requestsDto = LoginUserRequestsDto.builder()
                        .email("ekrrrdj2@jnu.ac.kr")
                        .pwd("1234")
                        .build();
                String requestBody = om.writeValueAsString(requestsDto);
                // when
                ResultActions resultActions = mvc.perform(post("/api/user/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                );
                // eye
                String responseBody = resultActions.andReturn().getResponse().getContentAsString();
                // then
                resultActions.andExpectAll(
                        status().isOk(),
                        jsonPath("$.success").value(true),
                        jsonPath("$.response.accessToken").value("아직 미구현")
                );
            }
        }
    }
}
