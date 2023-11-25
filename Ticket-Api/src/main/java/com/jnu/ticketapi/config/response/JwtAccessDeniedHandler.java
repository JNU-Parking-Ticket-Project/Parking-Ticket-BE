package com.jnu.ticketapi.config.response;


import com.jnu.ticketcommon.exception.GlobalErrorCode;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {
    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException)
            throws IOException, ServletException {
        // 인증은 되었지만 충분한 권한이 없는 경 403
        response.setCharacterEncoding("utf-8");
        response.sendError(
                GlobalErrorCode.AUTHORITY_NOT_VALID.getStatus(),
                GlobalErrorCode.AUTHORITY_NOT_VALID.getReason());
    }
}
