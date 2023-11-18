package com.jnu.ticketcommon.annotation;


import com.jnu.ticketcommon.interfaces.SwaggerExampleExceptions;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiErrorExceptionsExample {
    Class<? extends SwaggerExampleExceptions> value();
}
