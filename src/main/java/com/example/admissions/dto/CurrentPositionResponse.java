package com.example.admissions.dto;

/**
 * Response DTO for current position in the flow.
 *
 * @param userId           the user identifier
 * @param status           the current status ("completed" or "in_progress")
 * @param currentStepIndex 0-based index of the current step
 * @param currentStepId    identifier of the current step
 * @param currentStepName  name of the current step
 * @param currentTaskId    identifier of the current task
 * @param currentTaskName  name of the current task
 * @param completedTasks   number of completed tasks
 * @param totalTasks       total number of visible tasks
 */
public record CurrentPositionResponse(
        String userId,
        String status,
        Integer currentStepIndex,
        String currentStepId,
        String currentStepName,
        String currentTaskId,
        String currentTaskName,
        Integer completedTasks,
        Integer totalTasks
) {
    public static CurrentPositionResponse completed(String userId) {
        return new CurrentPositionResponse(
                userId,
                "completed",
                null, null, null, null, null,
                null, null
        );
    }
}

