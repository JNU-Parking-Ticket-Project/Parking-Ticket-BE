package com.jnu.ticketapi.common.actuator;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@Endpoint(id = "threadpool")
public class ThreadPoolConfig {

    private final ThreadPoolTaskExecutor threadPoolTaskExecutor;

    public ThreadPoolConfig(ThreadPoolTaskExecutor threadPoolTaskExecutor){
        this.threadPoolTaskExecutor = threadPoolTaskExecutor;
    }

    @ReadOperation
    public ThreadPoolStats threadPoolStats(){
        return new ThreadPoolStats(
                threadPoolTaskExecutor.getPoolSize(),
                threadPoolTaskExecutor.getActiveCount(),
                threadPoolTaskExecutor.getQueueSize(),
                threadPoolTaskExecutor.getCorePoolSize(),
                threadPoolTaskExecutor.getMaxPoolSize()
        );

    }
    public record ThreadPoolStats(
            int poolSize,
            int activeCount,
            int queueSize,
            int corePoolSize,
            int maxPoolSize) {
    }


}
