package com.example.admissions.dto;

import java.util.List;

/**
 * Response DTO for task completion.
 *
 * @param userId     the user identifier
 * @param taskName   the name of the completed task
 * @param results    list containing the task result (single item)
 * @param userStatus the overall user status after completion
 */
public record CompleteStepResponse(
        String userId,
        String taskName,
        List<TaskResultDto> results,
        String userStatus
) {
}

