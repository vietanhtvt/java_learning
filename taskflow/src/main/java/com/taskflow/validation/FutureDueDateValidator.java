package com.taskflow.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDate;

public class FutureDueDateValidator implements ConstraintValidator<FutureDueDate, LocalDate> {

    @Override
    public boolean isValid(LocalDate value, ConstraintValidatorContext context) {
        if (value == null) return true; // null은 @NotNull이 처리
        return !value.isBefore(LocalDate.now());
    }
}
