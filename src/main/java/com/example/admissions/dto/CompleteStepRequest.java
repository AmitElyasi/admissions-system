package com.example.admissions.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * Request DTO for completing a task.
 * Uses snake_case field names to match the existing API contract.
 *
 * @param userId     the user identifier (mapped from "user_id")
 * @param taskId     the ID or name of the task to complete (mapped from "task_id")
 * @param taskPayload the payload data for the task completion (mapped from "task_payload")
 */
public record CompleteStepRequest(
        @JsonProperty("user_id")
        @NotBlank(message = "user_id is required")
        String userId,
        @JsonProperty("task_id")
        @NotBlank(message = "task_id is required")
        String taskId,
        @JsonProperty("task_payload")
        @NotNull(message = "task_payload is required")
        Map<String, Object> taskPayload
) {
}

