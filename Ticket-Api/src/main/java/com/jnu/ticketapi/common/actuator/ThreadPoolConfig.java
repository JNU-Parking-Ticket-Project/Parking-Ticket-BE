package com.jnu.ticketapi.common.actuator;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

@Component
@Endpoint(id = "threadpool")
public class ThreadPoolConfig {

    @Autowired
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;

    @ReadOperation
    public ThreadPoolStats threadPoolStats() {
        return new ThreadPoolStats(
                threadPoolTaskExecutor.getPoolSize(),
                threadPoolTaskExecutor.getActiveCount(),
                threadPoolTaskExecutor.getQueueSize(),
                threadPoolTaskExecutor.getCorePoolSize(),
                threadPoolTaskExecutor.getMaxPoolSize());
    }

    public record ThreadPoolStats(
            int poolSize, int activeCount, int queueSize, int corePoolSize, int maxPoolSize) {}
}
