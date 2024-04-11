package com.jnu.ticketdomain.domains.registration.event;


import com.jnu.ticketdomain.common.domainEvent.DomainEvent;
import com.jnu.ticketdomain.domains.registration.domain.Registration;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class RegistrationPublishEvent extends DomainEvent {
    private final String email;
    private final String name;
    private final String status;
    private final Integer sequence;

    public static RegistrationPublishEvent of(Registration registration) {
        return RegistrationPublishEvent.builder()
                .email(registration.getEmail())
                .name(registration.getName())
                .status(registration.getUser().getStatus())
                .sequence(registration.getUser().getSequence())
                .build();
    }

    public static RegistrationPublishEvent of(
            Registration registration, String status, Integer sequence) {
        return RegistrationPublishEvent.builder()
                .email(registration.getEmail())
                .name(registration.getName())
                .status(status)
                .sequence(sequence)
                .build();
    }
}
