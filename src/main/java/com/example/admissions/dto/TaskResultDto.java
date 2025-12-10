package com.example.admissions.dto;

/**
 * DTO representing a task completion result.
 *
 * @param taskId   the identifier of the task
 * @param taskName the human-readable name of the task
 * @param passed   whether the task was passed
 */
public record TaskResultDto(String taskId, String taskName, boolean passed) {
}

