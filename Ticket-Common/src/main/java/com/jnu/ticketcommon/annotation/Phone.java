package com.jnu.ticketcommon.annotation;


import com.jnu.ticketcommon.validator.PhoneValidator;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.validation.Constraint;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PhoneValidator.class)
public @interface Phone {

    String message() default "올바른 형식(010-xxxx-xxxx)의 전화번호를 입력해주세요";

    Class[] groups() default {};

    Class[] payload() default {};
}
