package com.jnu.ticketdomain.domains.registration.domain;


import lombok.Builder;

@Builder
public record RegistrationResultEmailDto(
        String id,
        String receiverEmail,
        String receiverName,
        String registrationResult,
        Integer registrationSequence) {
    public static RegistrationResultEmailDto of(RegistrationResultEmail email) {
        return RegistrationResultEmailDto.builder()
                .id(email.getEmailId())
                .receiverEmail(email.getReceiverEmail())
                .receiverName(email.getReceiverName())
                .registrationResult(email.getRegistrationResult())
                .registrationSequence(email.getRegistrationSequence())
                .build();
    }
}
