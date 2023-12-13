package com.jnu.ticketcommon.annotation;

import com.jnu.ticketcommon.validator.PasswordValidator;

import javax.validation.Constraint;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PasswordValidator.class)
public @interface Password {

    String message() default "";

    Class[] groups() default {};

    Class[] payload() default {};
}
