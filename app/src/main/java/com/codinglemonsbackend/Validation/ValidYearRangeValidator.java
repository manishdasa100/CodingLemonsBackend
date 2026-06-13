package com.codinglemonsbackend.Validation;

import com.codinglemonsbackend.Dto.UserWorkExperienceDto;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidYearRangeValidator implements ConstraintValidator<ValidYearRange, UserWorkExperienceDto> {

    @Override
    public boolean isValid(UserWorkExperienceDto experience, ConstraintValidatorContext context) {
        if (experience == null) return true;
        Integer start = experience.getStartYear();
        Integer end = experience.getEndYear();
        if (start == null || end == null) return true;
        if (end < start) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("End year must not be before start year")
                   .addPropertyNode("endYear")
                   .addConstraintViolation();
            return false;
        }
        return true;
    }
}
