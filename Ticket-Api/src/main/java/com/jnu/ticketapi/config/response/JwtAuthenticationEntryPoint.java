package com.jnu.ticketapi.config.response;


import com.jnu.ticketcommon.exception.GlobalErrorCode;
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
        // 인증되지 않은 사용자가 보호된 리소스에 접근하려고 할 때 401
        response.setCharacterEncoding("utf-8");
        response.sendError(
                GlobalErrorCode.AUTHENTICATION_NOT_VALID.getStatus(),
                GlobalErrorCode.AUTHENTICATION_NOT_VALID.getReason());
    }
}
