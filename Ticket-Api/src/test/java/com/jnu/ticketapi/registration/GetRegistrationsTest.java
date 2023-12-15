package com.jnu.ticketapi.registration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jnu.ticketapi.RestDocsConfig;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@AutoConfigureMockMvc
@AutoConfigureRestDocs
@Sql("classpath:db/teardown.sql")
@WithMockUser(roles = "COUNCIL")
public class GetRegistrationsTest extends RestDocsConfig {
    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper om;

    @Nested
    class getRegistrationListTest {
        @Test
        @DisplayName("성공 : 신청 목록 조회")
        void success() throws Exception {
            //given

            //when
            ResultActions resultActions =
                    mvc.perform(
                            get("/v1/registrations")
                                    .contentType(MediaType.APPLICATION_JSON));

            // eye
            String responseBody = resultActions.andReturn().getResponse().getContentAsString();

            //then
            resultActions.andExpectAll(
                    status().isOk(),
                    jsonPath("$.registrations[0].email").value("user@jnu.ac.kr"),
                    jsonPath("$.registrations[1].email").value("council@jnu.ac.kr"));
            resultActions.andDo(MockMvcResultHandlers.print()).andDo(document);
            log.info("responseBody : {}", responseBody);
        }
    }
}

