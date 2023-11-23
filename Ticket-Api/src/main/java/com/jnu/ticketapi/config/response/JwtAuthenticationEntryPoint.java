package com.jnu.ticketapi.config.response;


import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {
    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException)
            throws IOException, ServletException {
        // 유효한 자격증명을 제공하지 않고 접근하려 할때 401
        response.setCharacterEncoding("utf-8");
        response.sendError(401, "인가되지 않은 사용자입니다.");
    }
}
