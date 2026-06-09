package com.codinglemonsbackend.Validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidYearRangeValidator.class)
@Documented
public @interface ValidYearRange {
    String message() default "End year must not be before start year";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
