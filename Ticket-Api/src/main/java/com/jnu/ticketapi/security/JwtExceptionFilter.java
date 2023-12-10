package com.jnu.ticketapi.security;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jnu.ticketcommon.exception.ErrorResponse;
import com.jnu.ticketcommon.exception.TicketCodeException;
import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

public class JwtExceptionFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        try {
            filterChain.doFilter(request, response);
        } catch (TicketCodeException e) {
            response.setStatus(e.getErrorReason().getStatus());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.isCommitted();
            objectMapper.writeValue(
                    response.getWriter(),
                    new ErrorResponse(
                            e.getErrorReason().getStatus(),
                            e.getErrorReason().getCode(),
                            e.getErrorReason().getReason(),
                            request.getRequestURL().toString()));
            return;
        }
    }
}
