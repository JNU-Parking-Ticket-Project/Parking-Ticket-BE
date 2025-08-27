package com.jnu.ticketinfrastructure.model;

import org.springframework.data.redis.connection.stream.RecordId;

public record RegistrationInFoRecord(RecordId recordId, RegistrationInfo registrationInfo) {

}
