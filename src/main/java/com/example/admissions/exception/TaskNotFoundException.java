package com.example.admissions.exception;

/**
 * Exception thrown when a requested task is not found in the system.
 */
public class TaskNotFoundException extends RuntimeException {
    public TaskNotFoundException(String taskId) {
        super("Task not found: " + taskId);
    }
}

