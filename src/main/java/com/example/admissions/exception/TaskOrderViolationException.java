package com.example.admissions.exception;

/**
 * Exception thrown when attempting to complete a task before completing prerequisite tasks in the same step.
 */
public class TaskOrderViolationException extends RuntimeException {
    public TaskOrderViolationException(String taskName, String prerequisiteTaskName) {
        super(String.format("Cannot complete task '%s' before completing prerequisite task '%s'", taskName, prerequisiteTaskName));
    }
}

