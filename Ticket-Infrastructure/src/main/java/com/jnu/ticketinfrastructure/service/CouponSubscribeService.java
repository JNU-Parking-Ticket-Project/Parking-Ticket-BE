package com.jnu.ticketinfrastructure.service;

import static com.jnu.ticketcommon.consts.TicketStatic.REDIS_COUPON_CHANNEL;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jnu.ticketinfrastructure.domainEvent.CouponIssuedEvent;
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
public class CouponSubscribeService implements MessageListener {
    @Autowired @Lazy private RedisTemplate<String, Object> redisTemplate;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void onMessage(Message message, byte[] pattern) {
        String channel = new String(message.getChannel());
        Long userId = null;
        try {
            JsonNode jsonNode = objectMapper.readTree(message.getBody());
            userId = jsonNode.get("userId").asLong();
        } catch (Exception e) {
            e.printStackTrace();
        }
        // "userId" 키의 값을 추출
        if (REDIS_COUPON_CHANNEL.equals(channel)) {
            handleReceivedUserId(userId);
        } else {
            log.error("Received message from unknown channel: {}", channel);
        }
    }

    public void handleReceivedUserId(Long userId) {
        eventPublisher.publishEvent(CouponIssuedEvent.from(userId));
    }
}
