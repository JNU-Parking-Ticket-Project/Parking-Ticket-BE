package com.jnu.ticketapi.config;

import static com.jnu.ticketcommon.consts.TicketStatic.SwaggerPatterns;

import com.jnu.ticketapi.config.response.JwtAccessDeniedHandler;
import com.jnu.ticketapi.config.response.JwtAuthenticationEntryPoint;
import com.jnu.ticketapi.security.JwtAuthenticationFilter;
import com.jnu.ticketapi.security.JwtExceptionFilter;
import com.jnu.ticketapi.security.JwtResolver;
import com.jnu.ticketcommon.helper.SpringEnvironmentHelper;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsUtils;

@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtResolver jwtResolver;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;
    private final Environment environment;
    private final SpringEnvironmentHelper springEnvironmentHelper;

    protected String[] councilAndAdminUrls = {
        "/v1/announce/**", "/v1/notice/**", "/v1/registrations", "/v1/sectors/**", "/v1/events/**"
    };

    protected String[] adminUrls = {
        "/v1/admin/**",
    };

    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // httpBasic, csrf, formLogin, rememberMe, logout, session disable

        http.httpBasic()
                .disable()
                .csrf()
                .disable()
                .formLogin()
                .disable()
                .rememberMe()
                .disable()
                .logout()
                .disable()
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .cors()
                .and()
                .exceptionHandling()
                .accessDeniedHandler(jwtAccessDeniedHandler)
                .and()
                .headers()
                .frameOptions()
                .sameOrigin();

        if (Arrays.asList(environment.getActiveProfiles()).contains("prod")) {
            http.authorizeRequests().mvcMatchers(SwaggerPatterns).authenticated().and().httpBasic();
        }
        // 요청에 대한 권한 설정
        http.authorizeRequests()
                .requestMatchers(CorsUtils::isPreFlightRequest)
                .permitAll()
                .antMatchers(
                        "/swagger-resources/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**",
                        "/v3/api-docs",
                        "/api-docs/**",
                        "/api-docs")
                .permitAll()
                .antMatchers(councilAndAdminUrls)
                .hasAnyRole("COUNCIL", "ADMIN")
                .antMatchers(adminUrls)
                .hasRole("ADMIN")
                .antMatchers("/v1/**")
                .authenticated()
                .anyRequest()
                .denyAll();

        // jwt filter 설정
        http.addFilterBefore(
                new JwtAuthenticationFilter(jwtResolver, springEnvironmentHelper),
                UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(new JwtExceptionFilter(), JwtAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return web ->
                web.ignoring()
                        .antMatchers(
                                HttpMethod.GET,
                                "/v1/notice",
                                "/v1/sectors",
                                "/v1/events/period",
                                "/actuator/**")
                        .antMatchers(HttpMethod.OPTIONS, "/v1/notice")
                        .antMatchers(HttpMethod.GET, "/v1/announce/**")
                        .antMatchers(HttpMethod.OPTIONS, "/v1/announce/**")
                        .antMatchers(
                                HttpMethod.POST,
                                "/v1/user/password/find",
                                "/v1/user/update/password/**")
                        .antMatchers(
                                HttpMethod.OPTIONS,
                                "/v1/user/password/find",
                                "/v1/user/update/password/**")
                        .antMatchers("/v1/auth/login/**")
                        .antMatchers("/v1/council/signup")
                        .antMatchers("/v1/auth/check/email/**")
                        .antMatchers("/error")
                        .requestMatchers(PathRequest.toStaticResources().atCommonLocations());
    }
}
