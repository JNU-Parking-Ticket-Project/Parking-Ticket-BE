package com.jnu.ticketapi.ticket;

import static com.jnu.ticketcommon.message.ResponseMessage.EVENT_SUCCESS_REGISTER_MESSAGE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jnu.ticketapi.api.event.controller.EventController;
import com.jnu.ticketapi.api.event.service.EventRegisterUseCase;
import com.jnu.ticketdomain.common.vo.DateTimePeriod;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class EventControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks private EventController eventController;

    @Mock private EventRegisterUseCase eventRegisterUseCase;

    private final MockMvc mockMvc;

    public EventControllerTest() {
        MockitoAnnotations.openMocks(this);
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        this.mockMvc = MockMvcBuilders.standaloneSetup(eventController).build();
    }

    @Test
    @DisplayName("성공 : 이벤트 등록")
    public void testSetEvent() throws Exception {
        // Given
        LocalDateTime startAt = LocalDateTime.now();
        LocalDateTime endAt = startAt.plusDays(1);
        DateTimePeriod dateTimePeriod =
                DateTimePeriod.builder().startAt(startAt).endAt(endAt).build();

        String successRegisterMessage = EVENT_SUCCESS_REGISTER_MESSAGE;

        // When
        ResultActions resultActions =
                mockMvc.perform(
                                post("/v1/events")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(dateTimePeriod)))
                        .andExpect(status().isOk());
        // Then
        resultActions.andExpect(status().isOk());
    }

    @Test
    void getEventOrder() {}

    @Test
    void openEventStatus() {}

    @Test
    void updateEventStatus() {}

    @Test
    void getEventPeriod() {}
}
