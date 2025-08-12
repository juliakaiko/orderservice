package com.mymicroservice.orderservice.util;

import com.mymicroservice.orderservice.annotations.CurrentDate;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDate;

public class CurrentDateValidator implements ConstraintValidator<CurrentDate, LocalDate> {

    @Override
    public boolean isValid(LocalDate value, ConstraintValidatorContext context) {
        if (value == null) {
            return false;
        }
        return value.isEqual(LocalDate.now());
    }
}
