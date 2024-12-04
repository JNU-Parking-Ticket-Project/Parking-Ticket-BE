package com.jnu.ticketapi.Announce.cache;


import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class CacheService {

    @Cacheable("myCache")
    public String getCachedData(String param) {
        simulateSlowService();
        return "Data for " + param;
    }

    private void simulateSlowService() {
        try {
            Thread.sleep(3000); // 3초 대기
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }
}
