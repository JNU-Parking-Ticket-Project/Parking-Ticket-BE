package com.jnu.ticketdomain.domains.events.event;


import com.jnu.ticketdomain.common.domainEvent.DomainEvent;
import com.jnu.ticketdomain.common.vo.DateTimePeriod;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class CouponExpiredEvent extends DomainEvent {
    private final DateTimePeriod dateTimePeriod;

    public static CouponExpiredEvent from(DateTimePeriod dateTimePeriod) {
        return CouponExpiredEvent.builder().dateTimePeriod(dateTimePeriod).build();
    }
}
