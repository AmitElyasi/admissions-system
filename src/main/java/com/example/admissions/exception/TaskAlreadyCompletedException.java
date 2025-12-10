package com.example.admissions.exception;

/**
 * Exception thrown when attempting to complete a task that has already been completed.
 */
public class TaskAlreadyCompletedException extends RuntimeException {
    public TaskAlreadyCompletedException(String taskName) {
        super("Task '" + taskName + "' has already been completed");
    }
}

