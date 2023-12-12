package com.jnu.ticketdomain.common.vo;


import java.time.LocalDateTime;
import java.time.ZoneId;
import javax.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DateTimePeriod {
    // 쿠폰 발행 시작 시각
    private LocalDateTime startAt;
    // 쿠폰 발행 마감 시각
    private LocalDateTime endAt;

    @Builder
    public DateTimePeriod(LocalDateTime startAt, LocalDateTime endAt) {
        // TimeZone UTC로 설정
        this.startAt = startAt.atZone(ZoneId.of("UTC")).toLocalDateTime();
        this.endAt = endAt.atZone(ZoneId.of("UTC")).toLocalDateTime();
    }

    public static DateTimePeriod between(LocalDateTime startAt, LocalDateTime endAt) {
        return new DateTimePeriod(startAt, endAt);
    }

    public boolean contains(LocalDateTime datetime) {
        return (datetime.isAfter(startAt) || datetime.equals(startAt))
                && (datetime.isBefore(endAt) || datetime.equals(endAt));
    }
}
