package com.jnu.ticketapi.config.response;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jnu.ticketcommon.exception.ErrorResponse;
import com.jnu.ticketcommon.exception.GlobalErrorCode;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class JwtAccessDeniedHandler implements AccessDeniedHandler {
    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException)
            throws IOException, ServletException {
        // 인증은 되었지만 충분한 권한이 없는 경 403
        //        response.setCharacterEncoding("utf-8");
        //        response.sendError(
        //                GlobalErrorCode.AUTHORITY_NOT_VALID.getStatus(),
        //                GlobalErrorCode.AUTHORITY_NOT_VALID.getReason());
        ObjectMapper objectMapper = new ObjectMapper();

        objectMapper.registerModule(new JavaTimeModule());
        response.setStatus(GlobalErrorCode.AUTHORITY_NOT_VALID.getStatus());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.isCommitted();
        objectMapper.writeValue(
                response.getWriter(),
                new ErrorResponse(
                        GlobalErrorCode.AUTHORITY_NOT_VALID.getStatus(),
                        GlobalErrorCode.AUTHORITY_NOT_VALID.getCode(),
                        GlobalErrorCode.AUTHORITY_NOT_VALID.getReason(),
                        request.getRequestURL().toString()));
        log.error("Access denied: {}", accessDeniedException.getMessage());
        log.info("response: {}", response.toString());
    }
}
