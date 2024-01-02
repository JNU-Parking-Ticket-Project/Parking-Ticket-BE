package com.jnu.ticketdomain.domains.registration.event;


import com.jnu.ticketdomain.common.domainEvent.DomainEvent;
import com.jnu.ticketdomain.domains.registration.domain.Registration;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class RegistrationCreationEvent extends DomainEvent {
    private final String email;
    private final String name;
    private final String status;

    public static RegistrationCreationEvent of(Registration registration) {
        return RegistrationCreationEvent.builder()
                .email(registration.getEmail())
                .name(registration.getName())
                .status(registration.getUser().getStatus())
                .build();
    }
    public static RegistrationCreationEvent of(Registration registration, String status) {
        return RegistrationCreationEvent.builder()
                .email(registration.getEmail())
                .name(registration.getName())
                .status(status)
                .build();
    }
}
