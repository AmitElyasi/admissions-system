package com.example.admissions.model;

import java.time.Instant;
import java.util.Map;

/**
 * Stores per-user task result summary.
 * Contains the task completion status, timestamp, and payload data.
 *
 * @param taskId  Identifier of the completed task
 * @param passed  Whether the task was passed (true) or failed (false)
 * @param when    Timestamp when the task was completed
 * @param payload Original payload data received for this task
 */
public record TaskResult(
        String taskId,
        boolean passed,
        Instant when,
        Map<String, Object> payload
) {
    public TaskResult {
        if (taskId == null || taskId.isBlank()) {
            throw new IllegalArgumentException("Task id cannot be null or blank");
        }
        if (when == null) {
            throw new IllegalArgumentException("Timestamp cannot be null");
        }
        payload = payload != null ? Map.copyOf(payload) : Map.of(); // Defensive copy
    }
}