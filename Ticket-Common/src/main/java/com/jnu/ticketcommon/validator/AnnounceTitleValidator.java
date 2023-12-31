package com.jnu.ticketcommon.validator;


import com.jnu.ticketcommon.annotation.AnnounceTitle;
import com.jnu.ticketcommon.annotation.Validator;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

@Validator
public class AnnounceTitleValidator implements ConstraintValidator<AnnounceTitle, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return false;
        }
        return value.matches(".{1,100}");
    }
}
