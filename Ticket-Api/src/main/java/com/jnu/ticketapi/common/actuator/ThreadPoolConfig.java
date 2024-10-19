package com.jnu.ticketapi.common.actuator;


import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

@Endpoint(id = "threadpool")
@RequiredArgsConstructor
@Component
public class ThreadPoolConfig {

    @Value("${thread.core-pool-size}")
    private int corePoolSize;

    @Value("${thread.max-pool-size}")
    private int maxPoolSize;

    @Value("${thread.queue-capacity}")
    private int queueCapacity;

    @Autowired @Lazy private ThreadPoolTaskExecutor threadPoolTaskExecutor;

    @ReadOperation
    public ThreadPoolStats threadPoolStats() {
        threadPoolTaskExecutor.setCorePoolSize(corePoolSize);
        threadPoolTaskExecutor.setMaxPoolSize(maxPoolSize);
        threadPoolTaskExecutor.setQueueCapacity(queueCapacity);
        threadPoolTaskExecutor.initialize();
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
