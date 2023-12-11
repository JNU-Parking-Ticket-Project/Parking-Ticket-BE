package com.jnu.ticketinfrastructure.service;


import static com.jnu.ticketcommon.consts.TicketStatic.REDIS_EVENT_CHANNEL;

import com.jnu.ticketinfrastructure.model.ChatMessage;
import com.jnu.ticketinfrastructure.redis.RedisRepository;
import java.util.LinkedList;
import java.util.Queue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class WaitingQueueService {
    private final RedisRepository redisRepository;

    public WaitingQueueService(RedisRepository redisRepository) {
        this.redisRepository = redisRepository;
    }

    public Boolean registerQueue(String key, Long userId) {
        Double score = (double) System.currentTimeMillis();
        boolean isPresent = redisRepository.zAddIfAbsent(key, userId, score);
        // 재고가 있어야 처리
        if (isPresent) {
            publishMessage(new ChatMessage(userId));
        }
        return true;
    }

    private void publishMessage(ChatMessage message) {
        redisRepository.converAndSend(REDIS_EVENT_CHANNEL, message);
    }

    public <T> Queue<T> getQueue(String key, long startRank, long endRank, Class<T> type) {
        return (Queue<T>) new LinkedList<>(redisRepository.zRange(key, startRank, endRank, type));
    }

    public <T> Queue<T> popQueue(String key, long count, Class<T> type) {
        Queue<T> set = redisRepository.zPopMin(key, count, type);
        return new LinkedList<>(set);
    }

    public Object popValue(String key) {
        return redisRepository.zPopMin(key);
    }

    public Long getWaitingOrder(String key, Object value) {
        return redisRepository.zRank(key, value);
    }
}
