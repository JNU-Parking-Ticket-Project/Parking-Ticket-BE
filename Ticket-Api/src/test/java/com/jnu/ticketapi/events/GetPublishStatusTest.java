package com.jnu.ticketapi.events;



import com.jnu.ticketapi.config.BaseIntegrationTest;
import com.jnu.ticketapi.security.JwtGenerator;
import com.jnu.ticketdomain.common.vo.DateTimePeriod;
import com.jnu.ticketdomain.domains.events.domain.Event;
import com.jnu.ticketdomain.domains.events.repository.EventRepository;
import com.jnu.ticketdomain.domains.user.domain.User;
import com.jnu.ticketdomain.domains.user.domain.UserRole;
import com.jnu.ticketdomain.domains.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.ResultActions;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
public class GetPublishStatusTest extends BaseIntegrationTest {

    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired private JwtGenerator jwtGenerator;

    @BeforeEach
    void init(){
        User user = User.builder()
                .email("admin@jnu.ac.kr")
                .pwd("asdfasdfasdf")
                .userRole(UserRole.ADMIN)
                .build();
        userRepository.saveAndFlush(user);


        Event event = new Event(DateTimePeriod.builder()
                .startAt(LocalDateTime.of(LocalDate.now(), LocalTime.of(0,0)))
                .endAt(LocalDateTime.of(LocalDate.now(), LocalTime.of(23,59)))
                .build(), null, "테스트 이벤트 제목");
        eventRepository.saveAndFlush(event);
    }

    @DisplayName("이벤트 PUBLISH 조회 테스트")
    @Test
    public void get_publish_success_test() throws Exception{
        // given
        Long eventId = 1L;
        String accessToken = jwtGenerator.generateAccessToken("admin@jnu.ac.kr", "ADMIN");

        // when
        ResultActions resultActions = mockMvc.perform(
                get("/v1/events/publish/1")
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
                jsonPath("$.publish").value(false));
    }
}
