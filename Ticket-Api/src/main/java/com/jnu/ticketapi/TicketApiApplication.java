package com.jnu.ticketapi;


import java.util.Arrays;
import java.util.TimeZone;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.env.Environment;
import org.springframework.retry.annotation.EnableRetry;

@EnableRetry
@SpringBootApplication
@RequiredArgsConstructor
@ComponentScan(basePackages = {"com.jnu"})
@Slf4j
public class TicketApiApplication implements ApplicationListener<ApplicationReadyEvent> {
    private final Environment environment;

    @PostConstruct
    void started() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
    }

    public static void main(String[] args) {
        SpringApplication.run(TicketApiApplication.class, args);
    }
    // 현재 활성화된 프로파일을 로그로 출력
    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        log.info("applicationReady status" + Arrays.toString(environment.getActiveProfiles()));
    }
}
