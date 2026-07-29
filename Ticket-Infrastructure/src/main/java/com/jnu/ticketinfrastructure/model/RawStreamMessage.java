package com.jnu.ticketinfrastructure.model;


import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class RawStreamMessage {
    private final String recordId;
    private final String payload;
}
