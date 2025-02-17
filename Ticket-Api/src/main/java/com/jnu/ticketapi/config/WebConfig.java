package com.jnu.ticketapi.config;


import com.jnu.ticketapi.common.aop.EmailMethodArgumentResolver;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Cors 설정 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {
    private final EmailMethodArgumentResolver emailMethodArgumentResolver;
    private final WebLoggingInterceptor webLoggingInterceptor;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(emailMethodArgumentResolver);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(webLoggingInterceptor);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(
                        "http://localhost:4200",
                        "https://apply.jnu-parking.com/",
                        "https://apply.dev.jnu-parking.com",
                        "https://manager.jnu-parking.com/",
                        "https://manager.dev.jnu-parking.com",
                        "http://168.131.34.108:10025", // 사용자 페이지
                        "http://168.131.34.108:10026", // 관리자 페이지
                        "https://jnu-parking.econovation.kr", // 사용자 페이지
                        "https://jnu-parking-manager.econovation.kr", // 관리자 페이지
                        "https://jnu-parking-api.econovation.kr" // API 서버 (swagger 요청 시 허용)
                        )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }
}
