package com.jnu.ticketapi.security;


import com.fasterxml.jackson.databind.ObjectMapper;
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
        try {
            filterChain.doFilter(request, response);
        } catch (TicketCodeException e) {
            response.setStatus(e.getErrorReason().getStatus());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            objectMapper.writeValue(
                    response.getWriter(),
                    new ErrorResponse(e.getErrorReason(), request.getRequestURL().toString()));
        }
    }
}
