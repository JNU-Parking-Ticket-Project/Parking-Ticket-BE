package com.jnu.ticketcommon.annotation;


import com.jnu.ticketcommon.validator.AnnounceContentValidator;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.validation.Constraint;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = AnnounceContentValidator.class)
public @interface AnnounceContent {
    String message() default "";

    Class[] groups() default {};

    Class[] payload() default {};
}
