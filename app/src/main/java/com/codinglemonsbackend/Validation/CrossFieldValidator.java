package com.codinglemonsbackend.Validation;

import java.lang.reflect.Field;
import java.util.Arrays;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CrossFieldValidator implements ConstraintValidator<CrossFieldValidation, Object>{

    private CrossFieldValidation.FieldRule[] rules;
    
    @Override
    public void initialize(CrossFieldValidation constraintAnnotation) {
        this.rules = constraintAnnotation.rules();
    }
    
    @Override
    public boolean isValid(Object object, ConstraintValidatorContext context) {
        if (object == null) {
            return true;
        }
        
        boolean isValid = true;
        
        // Disable default constraint violation to provide custom messages per field
        context.disableDefaultConstraintViolation();
        
        for (CrossFieldValidation.FieldRule rule : rules) {
            try {
                Object fieldValue = getFieldValue(object, rule.field());
                Object dependsOnValue = getFieldValue(object, rule.dependsOn());
                
                if (!validateRule(rule, fieldValue, dependsOnValue)) {
                    isValid = false;
                    
                    String message = rule.message().isEmpty() ? 
                        getDefaultMessage(rule) : rule.message();
                    
                    // Add violation to the specific field
                    context.buildConstraintViolationWithTemplate(message)
                           .addPropertyNode(rule.field())
                           .addConstraintViolation();
                }
                
            } catch (Exception e) {
                // Log error in real application
                System.err.println("CrossFieldValidator error for field " + rule.field() + ": " + e.getMessage());
                // Continue validation for other rules
            }
        }
        
        return isValid;
    }
    
    private Object getFieldValue(Object object, String fieldName) throws Exception {
        Field field = findField(object.getClass(), fieldName);
        if (field == null) {
            throw new NoSuchFieldException("Field not found: " + fieldName);
        }
        
        field.setAccessible(true);
        return field.get(object);
    }
    
    private Field findField(Class<?> clazz, String fieldName) {
        // Search in current class and parent classes
        Class<?> searchType = clazz;
        while (searchType != null) {
            try {
                return searchType.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                // Try parent class
                searchType = searchType.getSuperclass();
            }
        }
        return null;
    }
    
    private boolean validateRule(CrossFieldValidation.FieldRule rule, Object fieldValue, Object dependsOnValue) {
        boolean currentEmpty = isEmpty(fieldValue);
        boolean dependsOnEmpty = isEmpty(dependsOnValue);
        String dependsOnString = dependsOnValue != null ? dependsOnValue.toString() : "";
        
        switch (rule.type()) {
            case REQUIRED_IF_NOT_EMPTY:
                // If dependsOn has value, current field must also have value
                return dependsOnEmpty || !currentEmpty;
                
            case FORBIDDEN_IF_EMPTY:
                // If dependsOn is empty, current field must also be empty
                return !dependsOnEmpty || currentEmpty;
                
            case BOTH_OR_NEITHER:
                // Both fields must be empty or both must have values
                return (currentEmpty && dependsOnEmpty) || (!currentEmpty && !dependsOnEmpty);
                
            case REQUIRED_IF_VALUES:
                // If dependsOn has specific values, current field is required
                boolean hasMatchingValue = Arrays.asList(rule.values()).contains(dependsOnString);
                return !hasMatchingValue || !currentEmpty;
                
            case FORBIDDEN_IF_VALUES:
                // If dependsOn has specific values, current field must be empty
                boolean hasForbiddenValue = Arrays.asList(rule.values()).contains(dependsOnString);
                return !hasForbiddenValue || currentEmpty;
                
            default:
                return true;
        }
    }
    
    private String getDefaultMessage(CrossFieldValidation.FieldRule rule) {
        switch (rule.type()) {
            case BOTH_OR_NEITHER:
                return rule.field() + " and " + rule.dependsOn() + " must be provided together or both must be empty";
            case REQUIRED_IF_NOT_EMPTY:
                return rule.field() + " is required when " + rule.dependsOn() + " is provided";
            case FORBIDDEN_IF_EMPTY:
                return rule.field() + " is not allowed when " + rule.dependsOn() + " is empty";
            case REQUIRED_IF_VALUES:
                return rule.field() + " is required when " + rule.dependsOn() + " has values: " + Arrays.toString(rule.values());
            case FORBIDDEN_IF_VALUES:
                return rule.field() + " is forbidden when " + rule.dependsOn() + " has values: " + Arrays.toString(rule.values());
            default:
                return "Cross field validation failed for " + rule.field();
        }
    }
    
    private boolean isEmpty(Object value) {
        if (value == null) return true;
        if (value instanceof String) return ((String) value).trim().isEmpty();
        if (value instanceof Object[]) return ((Object[]) value).length == 0;
        return false;
    }
    

}
