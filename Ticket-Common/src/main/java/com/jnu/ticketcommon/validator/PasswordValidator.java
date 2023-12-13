package com.jnu.ticketcommon.validator;

import com.jnu.ticketcommon.annotation.Password;
import com.jnu.ticketcommon.annotation.Validator;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
@Validator
public class PasswordValidator implements ConstraintValidator<Password, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return false;
        }
       return value.matches("^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[@#$%^&+=!]).{8,}$");
    }
}