package com.jnu.ticketapi.api.registration.model.response;


import lombok.Builder;

@Builder
public record FinalSaveResponse(Long registrationId, String message) {}
