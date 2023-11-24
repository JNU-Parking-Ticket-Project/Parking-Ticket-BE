package com.jnu.ticketapi.adapter.in;

import com.jnu.ticketapi.application.port.ScheduleUseCase;
import com.jnu.ticketapi.dto.ScheduleRequestDto;
import com.jnu.ticketapi.dto.ScheduleResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class SystemController {

    private final ScheduleUseCase scheduleUseCase;

    @PostMapping("/schedule")
    public ResponseEntity<ScheduleResponseDto> createSchedule(
            @RequestBody ScheduleRequestDto scheduleRequestDto)
    {
        ScheduleResponseDto scheduleResponseDto =
                scheduleUseCase.createSchedule(scheduleRequestDto);
        return ResponseEntity.ok(scheduleResponseDto);

    }
}
