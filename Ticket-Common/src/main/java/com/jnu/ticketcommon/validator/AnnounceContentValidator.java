package com.jnu.ticketcommon.validator;


import com.jnu.ticketcommon.annotation.AnnounceContent;
import com.jnu.ticketcommon.annotation.Validator;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

@Validator
public class AnnounceContentValidator implements ConstraintValidator<AnnounceContent, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return false;
        }
        return value.matches("(.|\\n){1,10000}");
    }
}
