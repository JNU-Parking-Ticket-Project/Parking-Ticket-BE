package com.jnu.ticketapi.config;


import com.jnu.ticketapi.config.response.JwtAccessDeniedHandler;
import com.jnu.ticketapi.config.response.JwtAuthenticationEntryPoint;
import com.jnu.ticketapi.security.JwtAuthenticationFilter;
import com.jnu.ticketapi.security.JwtExceptionFilter;
import com.jnu.ticketapi.security.JwtResolver;
import java.util.List;
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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtResolver jwtResolver;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;

    protected String[] councilAndAdminUrls = {
        "/v1/announce/**", "/v1/notice/**", "/v1/registrations", "/v1/sectors/**", "/v1/sectors"
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
                .configurationSource(corsConfigurationSource())
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
                new JwtAuthenticationFilter(jwtResolver),
                UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(new JwtExceptionFilter(), JwtAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return web ->
                web.ignoring()
                        .antMatchers(HttpMethod.GET, "/v1/notice")
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
    /*
    Spring Mvc CORS 설정을 해주면 corsConfigurationSource() 메서드를 구현할 필요가 없다고는 하는데
    혹시몰라서 일단 작성해놓음
    */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(List.of("http://localhost:4200"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
