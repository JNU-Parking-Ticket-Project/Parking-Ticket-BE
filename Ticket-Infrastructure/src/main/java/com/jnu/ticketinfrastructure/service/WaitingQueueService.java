package com.jnu.ticketinfrastructure.service;


import com.jnu.ticketinfrastructure.redis.RedisRepository;
import java.util.LinkedList;
import java.util.Queue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
// import org.springframework.messaging.simp.SimpMessagingTemplate;

@Service
@Slf4j
public class WaitingQueueService {
    private final RedisRepository redisRepository;
    //        private final SimpMessagingTemplate messagingTemplate;

    public WaitingQueueService(RedisRepository redisRepository) {
        this.redisRepository = redisRepository;
    }

    public Boolean registerQueue(String key, Object value) {
        Double score = (double) System.currentTimeMillis();
        Boolean result = redisRepository.zAddIfAbsent(key, value, score);
        if (result) {
            notifyQueueUpdate(key);
        }
        return result;
    }

    private void notifyQueueUpdate(String key) {
        //        messagingTemplate.convertAndSend(REDIS_COUPON_CHANNEL, key);
    }

    public <T> Queue<T> getQueue(String key, long startRank, long endRank, Class<T> type) {
        return (Queue<T>) new LinkedList<>(redisRepository.zRange(key, startRank, endRank, type));
    }

    public <T> Queue<T> popQueue(String key, long count, Class<T> type) {
        return new LinkedList<>(redisRepository.zPopMin(key, count, type));
    }

    public Long getWaitingOrder(String key, Object value) {
        return redisRepository.zRank(key, value);
    }
}
