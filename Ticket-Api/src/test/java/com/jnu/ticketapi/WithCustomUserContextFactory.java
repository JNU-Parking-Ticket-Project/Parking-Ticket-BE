package com.jnu.ticketapi;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.List;

@ActiveProfiles("test")
public class WithCustomUserContextFactory implements WithSecurityContextFactory<WithCustomMockUser> {

    @Override
    public SecurityContext createSecurityContext(WithCustomMockUser annotation) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();

        List<SimpleGrantedAuthority> authorities = Arrays.stream(annotation.roles())
                .map(role -> "ROLE_" + role)
                .map(SimpleGrantedAuthority::new)
                .toList();
        String username = String.valueOf(annotation.id());

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                username,
                "",
                authorities
        );

        context.setAuthentication(authentication);
        return context;
    }
}