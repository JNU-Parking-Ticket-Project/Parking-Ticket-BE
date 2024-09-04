package com.jnu.ticketinfrastructure.config;


import com.jnu.ticketinfrastructure.slack.CustomAsyncExceptionHandler;
import java.util.concurrent.Executor;
import lombok.RequiredArgsConstructor;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@EnableAsync
@Configuration
@RequiredArgsConstructor
public class EnableAsyncConfig implements AsyncConfigurer {

    private final CustomAsyncExceptionHandler customAsyncExceptionHandler;

    @Autowired private ThreadPoolTaskExecutor executor;

    @Value("${thread.core-pool-size}")
    private int corePoolSize;

    @Value("${thread.max-pool-size}")
    private int maxPoolSize;

    @Value("${thread.queue-capacity}")
    private int queueCapacity;

    @Override
    public Executor getAsyncExecutor() {
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.initialize();
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return customAsyncExceptionHandler;
    }
}
