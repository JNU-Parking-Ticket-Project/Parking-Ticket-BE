package com.jnu.ticketdomain.domains.user.event;


import com.jnu.ticketdomain.common.domainEvent.DomainEvent;
import com.jnu.ticketdomain.domains.events.domain.Sector;
import com.jnu.ticketdomain.domains.registration.domain.Registration;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class UserReflectStatusEvent extends DomainEvent {
    private Long userId;
    private Registration registration;
    private Sector sector;

    public static UserReflectStatusEvent of(Long userId, Registration registration, Sector sector) {
        return UserReflectStatusEvent.builder()
                .userId(userId)
                .registration(registration)
                .sector(sector)
                .build();
    }
}
