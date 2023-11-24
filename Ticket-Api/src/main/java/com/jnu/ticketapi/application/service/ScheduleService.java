package com.jnu.ticketapi.application.service;

import com.jnu.ticketapi.application.port.ScheduleUseCase;
import com.jnu.ticketapi.dto.ScheduleRequestDto;
import com.jnu.ticketapi.dto.ScheduleResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ScheduleService implements ScheduleUseCase {
    @Override
    public ScheduleResponseDto createSchedule(ScheduleRequestDto scheduleRequestDto) {
        return null;
    }
}
