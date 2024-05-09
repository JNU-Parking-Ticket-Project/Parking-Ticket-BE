package com.jnu.ticketapi.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jnu.ticketapi.api.user.model.request.UpdatePasswordRequest;
import com.jnu.ticketdomain.domains.CredentialCode.domain.CredentialCode;
import com.jnu.ticketdomain.domains.CredentialCode.repository.CredentialCodeRepository;
import com.jnu.ticketdomain.domains.user.domain.User;
import com.jnu.ticketdomain.domains.user.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
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
public class UpdatePasswordTest {

    @Autowired private MockMvc mvc;

    @Autowired private ObjectMapper om;
    @Autowired private CredentialCodeRepository credentialCodeRepository;
    @Autowired private UserRepository userRepository;

    public static final String CODE = UUID.randomUUID().toString();

    @BeforeEach
    public void setUp() {

        User user = User.builder().email("pon05114@naver.com").pwd("testPassword").build();
        userRepository.saveAndFlush(user);
        CredentialCode credentialCode =
                CredentialCode.builder().email("pon05114@naver.com").code(CODE).build();
        credentialCodeRepository.saveAndFlush(credentialCode);
    }

    @Test
    @DisplayName("성공 : 비밀번호 재설정")
    @WithMockUser(roles = "COUNCIL")
    void update_password_test() throws Exception {
        // given

        UpdatePasswordRequest updatePasswordRequest =
                UpdatePasswordRequest.builder().password("QKrdudrb@123").build();
        String requestBody = om.writeValueAsString(updatePasswordRequest);

        // when
        ResultActions resultActions =
                mvc.perform(
                        post("/v1/user/update/password/" + CODE)
                                .contentType(MediaType.APPLICATION_JSON)
                                .characterEncoding(StandardCharsets.UTF_8)
                                .content(requestBody));

        // eye
        String responseBody =
                resultActions.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        log.info("responseBody : " + responseBody);

        // then
        resultActions.andExpectAll(
                status().isOk(), jsonPath("$.email").value("pon05114@naver.com"));
    }
}
