package com.jnu.ticketapi.api.event.model.response;


import com.jnu.ticketdomain.common.vo.DateTimePeriod;
import com.jnu.ticketdomain.domains.events.domain.Event;
import java.time.format.DateTimeFormatter;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;

public record EventsInfoResponse(
        Long eventId,
        String eventTitle,
        String eventStatus,
        String dateTimePeriod,
        boolean publish) {

    @Builder(access = AccessLevel.PACKAGE)
    public EventsInfoResponse {}

    public static EventsInfoResponse from(@NotNull Event event) {
        return EventsInfoResponse.builder()
                .eventId(event.getId())
                .eventTitle(event.getTitle())
                .eventStatus(event.getEventStatus().toString())
                .dateTimePeriod(EventsInfoResponse.toString(event.getDateTimePeriod()))
                .publish(event.getPublish())
                .build();
    }

    private static String toString(DateTimePeriod dateTimePeriod) {
        return dateTimePeriod.getStartAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                + " - "
                + dateTimePeriod.getEndAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }
}
