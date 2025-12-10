package com.example.admissions.model;

import java.util.Map;

/**
 * Lightweight immutable snapshot of a user's state.
 * Used by visibility predicates to determine which tasks should be visible.
 *
 * @param userId         Identifier of the user
 * @param completedTasks Map of task ID to task result for all completed tasks
 */
public record UserStateSnapshot(
        String userId,
        Map<String, TaskResult> completedTasks
) {
    public UserStateSnapshot {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("User id cannot be null or blank");
        }
        completedTasks = completedTasks != null ? Map.copyOf(completedTasks) : Map.of(); // Defensive copy
    }
}