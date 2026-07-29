package com.jnu.ticketinfrastructure.stream;


import com.jnu.ticketinfrastructure.model.StreamQueueMessage;

public interface RegistrationStreamMessageHandler {
    void handle(StreamQueueMessage streamQueueMessage);
}
