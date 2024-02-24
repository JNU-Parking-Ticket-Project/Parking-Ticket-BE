package com.jnu.ticketapi.api.event.model.request;

import lombok.Builder;

@Builder
public record UpdateEventPublishRequest(boolean publish) {
}
