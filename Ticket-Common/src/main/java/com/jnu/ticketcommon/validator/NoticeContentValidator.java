package com.jnu.ticketcommon.validator;


import com.jnu.ticketcommon.annotation.NoticeContent;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class NoticeContentValidator implements ConstraintValidator<NoticeContent, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return false;
        }
        return value.matches("(.|\\n){1,10000}");
    }
}
