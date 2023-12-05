package com.jnu.ticketapi;


import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.env.Environment;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication
@RequiredArgsConstructor
@ComponentScan(basePackages = {"com.jnu"})
@Slf4j
public class TicketApiApplication implements ApplicationListener<ApplicationReadyEvent> {
    private final Environment environment;

    public static void main(String[] args) {
        SpringApplication.run(TicketApiApplication.class, args);
    }
    // 현재 활성화된 프로파일을 로그로 출력
    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        log.info("applicationReady status" + Arrays.toString(environment.getActiveProfiles()));
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() { // 전역적으로 cors에러 해결법 Spring mvc에서 사용
        return new WebMvcConfigurer() {
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedOrigins("http://localhost:4200")
                        .allowedHeaders("*")
                        .allowCredentials(true);
            }
        };
    }
}
