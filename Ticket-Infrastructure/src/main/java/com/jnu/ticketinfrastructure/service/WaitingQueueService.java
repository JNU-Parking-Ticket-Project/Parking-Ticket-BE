package com.jnu.ticketinfrastructure.service;

import static com.jnu.ticketcommon.consts.TicketStatic.REDIS_COUPON_CHANNEL;

import com.jnu.ticketinfrastructure.model.ChatMessage;
import com.jnu.ticketinfrastructure.redis.RedisRepository;
import java.util.LinkedList;
import java.util.Queue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.DefaultTypedTuple;
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
        Boolean result = redisRepository.zAddIfAbsent(key, userId, score);
        if (result) {
            publishMessage(new ChatMessage(userId));
        }
        return result;
    }

    private void publishMessage(ChatMessage message) {
        redisRepository.converAndSend(REDIS_COUPON_CHANNEL, message);
    }

    public <T> Queue<T> getQueue(String key, long startRank, long endRank, Class<T> type) {
        return (Queue<T>) new LinkedList<>(redisRepository.zRange(key, startRank, endRank, type));
    }

    //    public <T> Queue<T> popQueue(String key, long count, Class<T> type) {
    //        return new LinkedList<>(redisRepository.zPopMin(key, count, type));
    //    }
    public <T> Queue<T> popQueue(String key, long count, Class<T> type) {
        Queue<DefaultTypedTuple> set = redisRepository.zPopMin(key, count, DefaultTypedTuple.class);
        Queue<T> queue = new LinkedList<>();
        set.forEach(
                tuple -> {
                    // 직접 변환 코드 추가
                    String userId = tuple.getValue().toString();
                    ChatMessage chatMessage = new ChatMessage(Long.getLong(userId));
                    queue.add((T) chatMessage);
                });
        return queue;
    }

    public Long getWaitingOrder(String key, Object value) {
        return redisRepository.zRank(key, value);
    }
}
