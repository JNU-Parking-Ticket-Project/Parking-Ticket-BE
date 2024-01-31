package com.jnu.ticketapi.events;

import static com.jnu.ticketcommon.message.ResponseMessage.PUBLISH_SUCCESS_TRUE_MESSAGE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jnu.ticketapi.config.BaseIntegrationTest;
import com.jnu.ticketapi.security.JwtGenerator;
import com.jnu.ticketdomain.common.vo.DateTimePeriod;
import com.jnu.ticketdomain.domains.events.domain.Event;
import com.jnu.ticketdomain.domains.events.repository.EventRepository;
import com.jnu.ticketdomain.domains.user.domain.User;
import com.jnu.ticketdomain.domains.user.domain.UserRole;
import com.jnu.ticketdomain.domains.user.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

@Slf4j
public class UpdatePublishStatusTest extends BaseIntegrationTest {

    @Autowired private EventRepository eventRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private JwtGenerator jwtGenerator;

    @BeforeEach
    void init() {
        User user =
                User.builder()
                        .email("admin@jnu.ac.kr")
                        .pwd("asdfasdfasdf")
                        .userRole(UserRole.ADMIN)
                        .build();
        userRepository.saveAndFlush(user);

        Event event =
                new Event(
                        DateTimePeriod.builder()
                                .startAt(LocalDateTime.of(LocalDate.now(), LocalTime.of(0, 0)))
                                .endAt(LocalDateTime.of(LocalDate.now(), LocalTime.of(23, 59)))
                                .build(),
                        null,
                        "테스트 이벤트 제목");
        eventRepository.saveAndFlush(event);
    }

    @DisplayName("PUBLISH 상태 변경 성공 테스트")
    @Test
    public void update_publish_status_test() throws Exception {
        // given
        String accessToken = jwtGenerator.generateAccessToken("admin@jnu.ac.kr", "ADMIN");

        // when
        ResultActions resultActions =
                mockMvc.perform(
                        post("/v1/events/publish/1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer " + accessToken)
                                .characterEncoding(StandardCharsets.UTF_8));

        // stub
        String responseBody =
                resultActions.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        log.info("responseBody : " + responseBody);

        // then

        resultActions.andExpectAll(
                status().is2xxSuccessful(),
                jsonPath("$.message").value(PUBLISH_SUCCESS_TRUE_MESSAGE));
    }
}
