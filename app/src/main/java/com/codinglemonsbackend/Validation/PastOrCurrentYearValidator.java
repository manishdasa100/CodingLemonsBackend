package com.codinglemonsbackend.Validation;

import java.time.Year;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PastOrCurrentYearValidator implements ConstraintValidator<PastOrCurrentYear, Integer> {

    @Override
    public boolean isValid(Integer year, ConstraintValidatorContext context) {
        if (year == null) return true; // @NotNull handles the null case
        return year <= Year.now().getValue();
    }
}
