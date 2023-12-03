package com.jnu.ticketinfrastructure.service;


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

    public Boolean registerQueue(String key, Object value) {
        Double score = (double) System.currentTimeMillis();
        Boolean result = redisRepository.zAddIfAbsent(key, value, score);
        log.info("register queue key: " + key + ", value: " + value + ", result " + result);
        return result;
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
