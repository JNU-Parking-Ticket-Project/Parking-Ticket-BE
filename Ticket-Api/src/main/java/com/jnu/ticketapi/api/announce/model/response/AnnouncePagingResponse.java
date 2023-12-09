package com.jnu.ticketapi.api.announce.model.response;


import com.jnu.ticketdomain.domains.announce.domain.Announce;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Builder;
import org.springframework.data.domain.Page;

public record AnnouncePagingResponse(
        List<AnnounceInfoResponse> announces, Long lastPage, Long nextPage) {

    private static final long LAST_PAGE = -1L;

    @Builder
    public AnnouncePagingResponse {}

    public static AnnouncePagingResponse of(Page<Announce> announcePaging) {
        if (!announcePaging.hasNext()) {
            return AnnouncePagingResponse.newLastScroll(
                    announcePaging.getContent(), announcePaging.getTotalPages() - 1);
        }
        return AnnouncePagingResponse.newPagingHasNext(
                announcePaging.getContent(),
                announcePaging.getTotalPages() - 1,
                announcePaging.getPageable().getPageNumber() + 1);
    }

    private static AnnouncePagingResponse newLastScroll(
            List<Announce> announcePaging, long lastPage) {
        return newPagingHasNext(announcePaging, lastPage, LAST_PAGE);
    }

    private static AnnouncePagingResponse newPagingHasNext(
            List<Announce> announcePaging, long lastPage, long nextPage) {
        return AnnouncePagingResponse.builder()
                .announces(getAnnounces(announcePaging))
                .lastPage(lastPage)
                .nextPage(nextPage)
                .build();
    }

    private static List<AnnounceInfoResponse> getAnnounces(List<Announce> announcePaging) {
        return announcePaging.stream().map(AnnounceInfoResponse::from).collect(Collectors.toList());
    }
}
