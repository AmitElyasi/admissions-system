package com.example.admissions.exception;

/**
 * Exception thrown when a requested user is not found in the system.
 */
public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String userId) {
        super("User not found: " + userId);
    }
}

