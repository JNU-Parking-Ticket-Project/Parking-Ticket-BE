package com.jnu.ticketapi.api.event.model.response;


import com.jnu.ticketdomain.domains.events.domain.Event;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Builder;
import org.springframework.data.domain.Page;

public record EventsPagingResponse(List<EventsInfoResponse> events, Long lastPage, Long nextPage) {

    private static final long LAST_PAGE = -1L;

    @Builder
    public EventsPagingResponse {}

    public static EventsPagingResponse of(Page<Event> eventPaging) {
        if (!eventPaging.hasNext()) {
            return EventsPagingResponse.newLastScroll(
                    eventPaging.getContent(), eventPaging.getTotalPages() - 1);
        }
        return EventsPagingResponse.newPagingHasNext(
                eventPaging.getContent(),
                eventPaging.getTotalPages() - 1,
                eventPaging.getPageable().getPageNumber() + 1);
    }

    private static EventsPagingResponse newLastScroll(List<Event> eventPaging, long lastPage) {
        return newPagingHasNext(eventPaging, lastPage, LAST_PAGE);
    }

    private static EventsPagingResponse newPagingHasNext(
            List<Event> eventPaging, long lastPage, long nextPage) {
        return EventsPagingResponse.builder()
                .events(getEvents(eventPaging))
                .lastPage(lastPage)
                .nextPage(nextPage)
                .build();
    }

    private static List<EventsInfoResponse> getEvents(List<Event> eventPaging) {
        return eventPaging.stream().map(EventsInfoResponse::from).collect(Collectors.toList());
    }
}
