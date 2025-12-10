package com.example.admissions.service;

import com.example.admissions.exception.DuplicateUserException;
import com.example.admissions.model.TaskResult;
import com.example.admissions.model.User;
import com.example.admissions.model.UserStateSnapshot;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Simple in-memory user store and helper methods.
 */
@Service
public class UserService {
    private final Map<String, User> users = new ConcurrentHashMap<>();
    private final Map<String, String> emailToId = new ConcurrentHashMap<>(); // email -> userId for duplicate checking
    private final AtomicLong idCounter = new AtomicLong(1); // Start from 1

    /**
     * Creates a new user with an incrementing numeric ID.
     * Email is normalized to lowercase to ensure uniqueness.
     *
     * @param email the user's email address
     * @return the created user
     * @throws DuplicateUserException if a user with this email already exists
     */
    public User createUser(String email) {
        String normalizedEmail = email.toLowerCase().trim();
        String userId = emailToId.get(normalizedEmail);
        if (userId != null) {
            throw new DuplicateUserException(email);
        }
        
        userId = String.valueOf(idCounter.getAndIncrement());
        User u = new User(userId, email);
        users.put(userId, u);
        emailToId.put(normalizedEmail, userId);
        return u;
    }

    public User getUser(String id) {
        return users.get(id);
    }

    /**
     * Adds a task result for a user. Uses timestamp from payload if provided, otherwise uses current time.
     *
     * @param userId the user identifier
     * @param taskId the task identifier
     * @param passed whether the task passed
     * @param payload the payload containing task data, may include "timestamp" field
     */
    public void addTaskResult(String userId, String taskId, boolean passed, Map<String,Object> payload) {
        User u = users.get(userId);
        if (u == null) return;
        
        Instant timestamp = parseTimestampFromPayload(payload);
        TaskResult tr = new TaskResult(taskId, passed, timestamp, payload);
        u.addTaskResult(tr);
    }

    /**
     * Parses timestamp from payload if present, otherwise returns current time.
     * Supports ISO-8601 strings and epoch seconds/milliseconds.
     */
    private Instant parseTimestampFromPayload(Map<String, Object> payload) {
        if (payload == null) {
            return Instant.now();
        }
        
        Object timestampObj = payload.get("timestamp");
        if (timestampObj == null) {
            return Instant.now();
        }
        
        // Try parsing as ISO-8601 string
        if (timestampObj instanceof String timestampStr) {
            // Handle common formats
            if ("now".equalsIgnoreCase(timestampStr.trim())) {
                return Instant.now();
            }
            
            try {
                // Try ISO-8601 format (e.g., "2025-12-10T15:00:00Z")
                return Instant.parse(timestampStr);
            } catch (DateTimeParseException e) {
                // Try other common formats if needed
                try {
                    // Try RFC-1123 format
                    return Instant.from(DateTimeFormatter.RFC_1123_DATE_TIME.parse(timestampStr));
                } catch (DateTimeParseException e2) {
                    // If parsing fails, fall back to current time
                    return Instant.now();
                }
            }
        }
        
        // Try parsing as number (epoch seconds or milliseconds)
        if (timestampObj instanceof Number number) {
            long epochMillis = number.longValue();
            // If less than year 2000 in milliseconds, assume it's in seconds
            if (epochMillis < 946684800000L) { // Jan 1, 2000 in milliseconds
                return Instant.ofEpochSecond(epochMillis);
            } else {
                return Instant.ofEpochMilli(epochMillis);
            }
        }
        
        // If we can't parse it, use current time
        return Instant.now();
    }

    public UserStateSnapshot snapshot(String userId) {
        User u = users.get(userId);
        if (u == null) return new UserStateSnapshot(userId, Map.of());
        return new UserStateSnapshot(userId, u.getCompletedTasks());
    }
}