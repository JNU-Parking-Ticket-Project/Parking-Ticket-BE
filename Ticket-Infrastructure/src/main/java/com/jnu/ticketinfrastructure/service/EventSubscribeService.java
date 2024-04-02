package com.jnu.ticketinfrastructure.service;

import static com.jnu.ticketcommon.consts.TicketStatic.REDIS_EVENT_CHANNEL;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jnu.ticketinfrastructure.domainEvent.EventIssuedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventSubscribeService implements MessageListener {
    @Autowired @Lazy private RedisTemplate<String, Object> redisTemplate;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void onMessage(Message message, byte[] pattern) {
        String channel = new String(message.getChannel());
        Long userId = null;
        Long eventId = null;
        try {
            JsonNode jsonNode = objectMapper.readTree(message.getBody());
            userId = jsonNode.get("userId").asLong();
            eventId = jsonNode.get("eventId").asLong();
        } catch (Exception e) {
            e.printStackTrace();
        }
        // "userId" 키의 값을 추출
        if (REDIS_EVENT_CHANNEL.equals(channel)) {
            handleReceivedUserId(userId, eventId);
        } else {
            log.error("Received message from unknown channel: {}", channel);
        }
    }

    public void handleReceivedUserId(Long userId, Long eventId) {
        eventPublisher.publishEvent(EventIssuedEvent.from(userId, eventId));
    }
}
