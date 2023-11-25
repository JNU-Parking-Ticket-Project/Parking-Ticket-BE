package com.jnu.ticketapi.config;


import com.jnu.ticketapi.config.response.JwtAccessDeniedHandler;
import com.jnu.ticketapi.config.response.JwtAuthenticationEntryPoint;
import com.jnu.ticketapi.security.JwtAuthenticationFilter;
import com.jnu.ticketapi.security.JwtExceptionFilter;
import com.jnu.ticketapi.security.JwtGenerator;
import com.jnu.ticketapi.security.JwtResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtGenerator jwtGenerator;
    private final JwtResolver jwtResolver;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;

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
                .exceptionHandling()
                .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                .accessDeniedHandler(jwtAccessDeniedHandler)
                .and()
                .headers()
                .frameOptions()
                .sameOrigin();

        // 요청에 대한 권한 설정
        http.authorizeRequests()
                .antMatchers(
                        "/swagger-resources/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**",
                        "/v3/api-docs",
                        "/api-docs/**",
                        "/api-docs")
                .permitAll()
                .antMatchers("/v1/**")
                .authenticated()
                .anyRequest()
                .denyAll();

        // jwt filter 설정
        http.addFilterBefore(
                new JwtAuthenticationFilter(jwtResolver),
                UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(new JwtExceptionFilter(), JwtAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) ->
                web.ignoring()
                        .antMatchers(HttpMethod.POST, "/v1/auth/login")
                        .antMatchers(HttpMethod.GET, "/error")
                        .requestMatchers(PathRequest.toStaticResources().atCommonLocations())
                        .requestMatchers(PathRequest.toH2Console());
    }
}
