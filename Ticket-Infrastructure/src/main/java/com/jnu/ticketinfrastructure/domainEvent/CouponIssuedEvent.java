package com.jnu.ticketinfrastructure.domainEvent;


import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class CouponIssuedEvent extends InfrastructureEvent {
    private final Long currentUserId;

    public static CouponIssuedEvent from(Long currentUserId) {
        return CouponIssuedEvent.builder().currentUserId(currentUserId).build();
    }
}
