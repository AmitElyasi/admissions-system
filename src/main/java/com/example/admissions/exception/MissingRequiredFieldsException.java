package com.example.admissions.exception;

import java.util.Set;

/**
 * Exception thrown when a payload is missing required fields for a task.
 */
public class MissingRequiredFieldsException extends RuntimeException {
    public MissingRequiredFieldsException(String taskName, Set<String> missingFields) {
        super(String.format("Task '%s' is missing required fields: %s", taskName, missingFields));
    }
}

