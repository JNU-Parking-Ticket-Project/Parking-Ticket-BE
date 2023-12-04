package com.jnu.ticketinfrastructure.service;

import static com.jnu.ticketcommon.consts.TicketStatic.REDIS_COUPON_CHANNEL;

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

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String channel = new String(message.getChannel());
        Long userId = (Long) redisTemplate.getValueSerializer().deserialize(message.getBody());
        if (REDIS_COUPON_CHANNEL.equals(channel)) {
            handleReceivedUserId(userId);
        } else {
            log.error("Received message from unknown channel: {}", channel);
        }
    }

    @Transactional
    public void handleReceivedUserId(Long userId) {
        eventPublisher.publishEvent(CouponIssuedEvent.builder().currentUserId(userId).build());
    }
}
