package com.jnu.ticketapi;


import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.env.Environment;

@SpringBootApplication
@RequiredArgsConstructor
@ComponentScan(basePackages = {"com.jnu.ticket*"})
@Slf4j
public class TicketApiApplication implements ApplicationListener<ApplicationReadyEvent> {
    private final Environment environment;

    public static void main(String[] args) {

        SpringApplication.run(TicketApiApplication.class, args);
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        log.info("applicationReady status" + Arrays.toString(environment.getActiveProfiles()));
    }
}
