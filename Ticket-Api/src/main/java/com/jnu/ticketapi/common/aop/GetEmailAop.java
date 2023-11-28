package com.jnu.ticketapi.common.aop;

import com.jnu.ticketapi.application.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import javax.servlet.http.HttpServletRequest;

@Component
@RequiredArgsConstructor
public class GetEmailAop implements HandlerMethodArgumentResolver {
    private final AuthService authService;
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        boolean hasEmailAnnotation = parameter.hasParameterAnnotation(GetEmail.class);
        boolean hasStringType = String.class.isAssignableFrom(parameter.getParameterType());
        return hasEmailAnnotation && hasStringType;
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
        HttpServletRequest request = (HttpServletRequest) webRequest.getNativeRequest();
        String requestAccessTokenInHeader = request.getHeader("Authorization");
        String requestAccessToken = authService.extractToken(requestAccessTokenInHeader);
        return authService.getPrincipal(requestAccessToken);
    }
}
