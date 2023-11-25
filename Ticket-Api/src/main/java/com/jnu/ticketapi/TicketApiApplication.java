package com.jnu.ticketapi;


import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.env.Environment;

@SpringBootApplication
@RequiredArgsConstructor
@ComponentScan(basePackages = {"com.jnu"})
@Slf4j
public class TicketApiApplication implements ApplicationListener<ApplicationReadyEvent> {
    private final Environment environment;

    @Value("${a.b}")
    private String a;

    public static void main(String[] args) {

        SpringApplication.run(TicketApiApplication.class, args);
    }
    // 현재 활성화된 프로파일을 로그로 출력
    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        log.info("applicationReady status" + Arrays.toString(environment.getActiveProfiles()));
    }
}
