package com.example.admissions.model;

import java.util.List;

/**
 * Step in the flow. Contains an ordered list of tasks.
 * A step with no tasks is interpreted as one implicit task.
 *
 * @param id    Unique identifier for the step
 * @param name  Human-readable name of the step
 * @param tasks Ordered list of tasks within this step
 */
public record Step(String id, String name, List<Task> tasks) {
    public Step {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Step id cannot be null or blank");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Step name cannot be null or blank");
        }
        if (tasks == null) {
            throw new IllegalArgumentException("Step tasks cannot be null");
        }
        tasks = List.copyOf(tasks); // Defensive copy for immutability
    }
}