package com.jnu.ticketinfrastructure.service;

import com.jnu.ticketinfrastructure.redis.RedisRepository;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedList;
import java.util.Queue;

@Service
public class WaitingQueueService {
    private final RedisRepository redisRepository;
    private final org.slf4j.Logger log = LoggerFactory.getLogger(this.getClass().getSimpleName());

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


