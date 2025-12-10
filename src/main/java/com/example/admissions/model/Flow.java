package com.example.admissions.model;

import java.util.List;

/**
 * Represents the complete admissions flow structure.
 * A flow consists of an ordered list of steps that users progress through.
 *
 * @param id    Unique identifier for the flow
 * @param name  Human-readable name of the flow
 * @param steps Ordered list of steps in the flow
 */
public record Flow(String id, String name, List<Step> steps) {
    public Flow {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Flow id cannot be null or blank");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Flow name cannot be null or blank");
        }
        if (steps == null) {
            throw new IllegalArgumentException("Flow steps cannot be null");
        }
        steps = List.copyOf(steps); // Defensive copy for immutability
    }
}