package com.jnu.ticketapi;

import org.springframework.security.test.context.support.WithSecurityContext;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithCustomUserContextFactory.class)
public @interface WithCustomMockUser {
    long id() default 1L;
    String[] roles() default {"TEST"};
}