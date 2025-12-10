package com.example.admissions.exception;

/**
 * Exception thrown when attempting to create a user with an email that already exists.
 */
public class DuplicateUserException extends RuntimeException {
    public DuplicateUserException(String email) {
        super("User with email already exists: " + email);
    }
}

