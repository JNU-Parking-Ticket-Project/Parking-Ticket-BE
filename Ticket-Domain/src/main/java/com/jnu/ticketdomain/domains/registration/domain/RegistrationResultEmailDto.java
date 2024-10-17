package com.jnu.ticketdomain.domains.registration.domain;

import lombok.Builder;

@Builder
public record RegistrationResultEmailDto(
    String id,
    String receiverEmail,
    String receiverName,
    String registrationResult,
    Integer registrationSequence
) {
    public static RegistrationResultEmailDto of(RegistrationResultEmailOutbox outbox) {
        return RegistrationResultEmailDto.builder()
            .id(outbox.getId())
            .receiverEmail(outbox.getReceiverEmail())
            .receiverName(outbox.getReceiverName())
            .registrationResult(outbox.getRegistrationResult())
            .registrationSequence(outbox.getRegistrationSequence())
            .build();
    }
}
