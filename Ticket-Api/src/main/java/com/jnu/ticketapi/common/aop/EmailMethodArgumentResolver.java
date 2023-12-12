package com.jnu.ticketapi.common.aop;


import com.jnu.ticketapi.security.JwtResolver;
import com.jnu.ticketcommon.consts.TicketStatic;
import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
@RequiredArgsConstructor
public class EmailMethodArgumentResolver implements HandlerMethodArgumentResolver {
    private final JwtResolver jwtResolver;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        boolean hasEmailAnnotation = parameter.hasParameterAnnotation(GetEmail.class);
        boolean hasStringType = String.class.isAssignableFrom(parameter.getParameterType());
        return hasEmailAnnotation && hasStringType;
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory)
            throws Exception {
        HttpServletRequest request = (HttpServletRequest) webRequest.getNativeRequest();
        String requestAccessTokenInHeader = request.getHeader("Authorization");
        String requestAccessToken = jwtResolver.extractToken(requestAccessTokenInHeader);
        return jwtResolver.parseClaims(requestAccessToken).get(TicketStatic.EMAIL_KEY).toString();
    }
}
