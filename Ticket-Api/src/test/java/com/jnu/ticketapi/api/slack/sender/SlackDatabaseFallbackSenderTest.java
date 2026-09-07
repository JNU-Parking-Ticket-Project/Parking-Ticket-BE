package com.jnu.ticketapi.api.slack.sender;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jnu.ticketapi.api.event.event.DatabaseFallbackActivatedEvent;
import com.jnu.ticketinfrastructure.slack.SlackErrorNotificationProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class SlackDatabaseFallbackSenderTest {

    @Test
    void sendsSlackNotificationForDatabaseFallbackTransition() {
        @SuppressWarnings("unchecked")
        ObjectProvider<SlackErrorNotificationProvider> providerHolder = mock(ObjectProvider.class);
        SlackErrorNotificationProvider provider = mock(SlackErrorNotificationProvider.class);
        when(providerHolder.getIfAvailable()).thenReturn(provider);
        SlackDatabaseFallbackSender sender = new SlackDatabaseFallbackSender(providerHolder);

        sender.handle(
                new DatabaseFallbackActivatedEvent(
                        10L, 8L, "RedisConnectionException: connection refused"));

        verify(provider).sendNotification(anyList());
    }

    @Test
    void skipsNotificationOutsideProductionProfile() {
        @SuppressWarnings("unchecked")
        ObjectProvider<SlackErrorNotificationProvider> providerHolder = mock(ObjectProvider.class);
        SlackDatabaseFallbackSender sender = new SlackDatabaseFallbackSender(providerHolder);

        sender.handle(new DatabaseFallbackActivatedEvent(10L, 8L, "test"));

        verify(providerHolder).getIfAvailable();
    }
}
