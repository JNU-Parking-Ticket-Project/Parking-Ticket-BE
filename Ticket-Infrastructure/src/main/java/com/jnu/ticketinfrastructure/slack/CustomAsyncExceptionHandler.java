package com.jnu.ticketinfrastructure.slack;


import java.lang.reflect.Method;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomAsyncExceptionHandler implements AsyncUncaughtExceptionHandler {
    private final Environment environment;
    private final SlackAsyncErrorSender slackAsyncErrorSender;

    @Override
    public void handleUncaughtException(Throwable throwable, Method method, Object... params) {
        log.error("Exception message - " + throwable);
        log.error("Method name - " + method.getName());
        for (Object param : params) {
            log.error("Parameter value - " + param);
        }
        if (Arrays.asList(environment.getActiveProfiles()).contains("prod")) {
            slackAsyncErrorSender.execute(method.getName(), throwable, params);
        }
    }
}
