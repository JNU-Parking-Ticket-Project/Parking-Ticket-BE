package com.jnu.ticketapi.api.event.model.request;


import com.jnu.ticketapi.api.sector.model.request.SectorRegisterRequest;
import com.jnu.ticketdomain.common.vo.DateTimePeriod;
import java.util.List;
import lombok.Builder;

public record EventRegisterRequest(
        DateTimePeriod dateTimePeriod, List<SectorRegisterRequest> sectors) {
    @Builder
    public EventRegisterRequest {}
}
