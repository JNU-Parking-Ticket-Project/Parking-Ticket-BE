package com.jnu.ticketapi.application.port;


import com.jnu.ticketapi.dto.ScheduleRequestDto;
import com.jnu.ticketapi.dto.ScheduleResponseDto;

public interface ScheduleUseCase {

    ScheduleResponseDto createSchedule(ScheduleRequestDto scheduleRequestDto);
}
