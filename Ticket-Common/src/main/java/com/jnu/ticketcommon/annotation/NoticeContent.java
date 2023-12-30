package com.jnu.ticketcommon.annotation;


import com.jnu.ticketcommon.validator.NoticeContentValidator;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.validation.Constraint;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = NoticeContentValidator.class)
public @interface NoticeContent {
    String message() default "";

    Class[] groups() default {};

    Class[] payload() default {};
}
