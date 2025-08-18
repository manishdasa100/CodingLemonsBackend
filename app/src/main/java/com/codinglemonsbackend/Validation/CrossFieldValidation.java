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
@Constraint(validatedBy = CrossFieldValidator.class)
@Documented
public @interface CrossFieldValidation {
    String message() default "Cross field validation failed";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    
    FieldRule[] rules();
    
    @interface FieldRule {
        String field();
        String dependsOn();
        ValidationType type();
        String[] values() default {};
        String message() default "";
    }
    
    enum ValidationType {
        REQUIRED_IF_NOT_EMPTY,    // Field required if dependsOn is not empty
        FORBIDDEN_IF_EMPTY,       // Field forbidden if dependsOn is empty  
        BOTH_OR_NEITHER,          // Both fields must be provided or both must be empty
        REQUIRED_IF_VALUES,       // Field required if dependsOn has specific values
        FORBIDDEN_IF_VALUES       // Field forbidden if dependsOn has specific values
    }
}
