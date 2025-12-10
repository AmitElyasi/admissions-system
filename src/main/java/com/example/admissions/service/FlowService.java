package com.example.admissions.service;

import com.example.admissions.dto.CompleteStepResponse;
import com.example.admissions.dto.CurrentPositionResponse;
import com.example.admissions.dto.TaskResultDto;
import com.example.admissions.dto.UserStatusResponse;
import com.example.admissions.exception.MissingRequiredFieldsException;
import com.example.admissions.exception.TaskAlreadyCompletedException;
import com.example.admissions.exception.TaskNotFoundException;
import com.example.admissions.exception.TaskOrderViolationException;
import com.example.admissions.exception.UserNotFoundException;
import com.example.admissions.model.Flow;
import com.example.admissions.model.Step;
import com.example.admissions.model.Task;
import com.example.admissions.model.TaskResult;
import com.example.admissions.model.UserStateSnapshot;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service for managing flow-related operations.
 * Handles computation of current position, visibility, and user status in the admissions flow.
 */
@Service
public class FlowService {
    private final Flow flow;
    private final UserService userService;

    public FlowService(Flow flow, UserService userService) {
        this.flow = flow;
        this.userService = userService;
    }

    /**
     * Returns the complete flow structure.
     *
     * @return the flow configuration
     */
    public Flow getFlow() {
        return flow;
    }

    /**
     * Returns the ordered list of steps in the flow.
     *
     * @return list of steps in order
     */
    public List<Step> getOrderedSteps() {
        return flow.steps();
    }

    /**
     * Computes which steps and tasks are visible for a given user.
     * Takes into account conditional tasks that may appear based on user state.
     *
     * @param userId the user identifier
     * @return list of steps with only visible tasks included
     */
    public List<Step> getVisibleStepsForUser(String userId) {
        UserStateSnapshot snapshot = userService.snapshot(userId);
        return flow.steps().stream()
                .map(step -> {
                    List<Task> visibleTasks = step.tasks().stream()
                            .filter(task -> task.isVisible(snapshot))
                            .toList();
                    return new Step(step.id(), step.name(), visibleTasks);
                })
                .toList();
    }

    /**
     * Computes the current step index (0-based) and current task for a user.
     * Prioritizes failed tasks (attempted but not passed) over incomplete tasks.
     * This allows users to retry failed tasks before moving to new ones.
     *
     * @param userId the user identifier
     * @return Optional containing the current position, or empty if all steps are completed
     */
    public Optional<CurrentPosition> computeCurrentPosition(String userId) {
        UserStateSnapshot snapshot = userService.snapshot(userId);
        List<Step> visibleSteps = getVisibleStepsForUser(userId);
        Map<String, TaskResult> completedTasks = snapshot.completedTasks();

        // First pass: find ALL failed tasks across all steps (prioritize retries)
        for (int stepIndex = 0; stepIndex < visibleSteps.size(); stepIndex++) {
            Step step = visibleSteps.get(stepIndex);
            for (Task task : step.tasks()) {
                TaskResult result = completedTasks.get(task.getId());
                if (result != null && !result.passed()) {
                    return Optional.of(new CurrentPosition(stepIndex, step, task));
                }
            }
        }

        // Second pass: find first incomplete task (normal progression)
        for (int stepIndex = 0; stepIndex < visibleSteps.size(); stepIndex++) {
            Step step = visibleSteps.get(stepIndex);
            for (Task task : step.tasks()) {
                if (!completedTasks.containsKey(task.getId())) {
                    return Optional.of(new CurrentPosition(stepIndex, step, task));
                }
            }
        }
        
        return Optional.empty(); // All steps completed
    }

    /**
     * Determines the user's acceptance status in the flow.
     * <ul>
     *   <li><b>rejected</b>: any completed task explicitly failed</li>
     *   <li><b>accepted</b>: all visible steps have all their tasks completed and passed</li>
     *   <li><b>in_progress</b>: otherwise</li>
     * </ul>
     *
     * @param userId the user identifier
     * @return the status string: "accepted", "rejected", or "in_progress"
     */
    public String userStatus(String userId) {
        UserStateSnapshot snapshot = userService.snapshot(userId);
        List<Step> visibleSteps = getVisibleStepsForUser(userId);
        Map<String, TaskResult> completedTasks = snapshot.completedTasks();

        // Check if any task failed
        boolean anyFailed = completedTasks.values().stream()
                .anyMatch(result -> !result.passed());
        if (anyFailed) {
            return "rejected";
        }

        // Check if all visible tasks are completed and passed
        boolean allCompleted = visibleSteps.stream()
                .flatMap(step -> step.tasks().stream())
                .allMatch(task -> {
                    TaskResult result = completedTasks.get(task.getId());
                    return result != null && result.passed();
                });

        return allCompleted ? "accepted" : "in_progress";
    }

    /**
     * Gets the user status response.
     * Validates that the user exists before returning status.
     *
     * @param userId the user identifier
     * @return response containing the user's status
     * @throws UserNotFoundException if the user does not exist
     */
    public UserStatusResponse getUserStatusResponse(String userId) {
        if (userService.getUser(userId) == null) {
            throw new UserNotFoundException(userId);
        }
        return new UserStatusResponse(userStatus(userId));
    }

    /**
     * Completes a task for a user by validating required fields and evaluating pass condition.
     *
     * @param userId     the user identifier
     * @param taskId     the ID or name of the task to complete
     * @param taskPayload the payload data for the task completion
     * @return response containing task result and updated user status
     * @throws TaskNotFoundException if the task is not found
     * @throws MissingRequiredFieldsException if required fields are missing
     * @throws TaskOrderViolationException if prerequisite tasks in the same step are not completed
     * @throws TaskAlreadyCompletedException if the task has already been completed
     */
    public CompleteStepResponse completeStep(String userId, String taskId, Map<String, Object> taskPayload) {
        Task task = findTask(taskId, userId);
        Step step = findStepContainingTask(task);
        
        // Check if task is already completed and not redoable
        UserStateSnapshot snapshot = userService.snapshot(userId);
        if (!task.isRedoable() && snapshot.completedTasks().containsKey(task.getId())) {
            throw new TaskAlreadyCompletedException(task.getName());
        }
        
        // Validate task order - ensure all previous tasks in the same step are completed
        validateTaskOrder(step, task, userId);
        
        // Validate that all required fields are present
        validateRequiredFields(task, taskPayload);
        
        // Evaluate if task passed
        boolean passed = task.evaluatePassed(taskPayload);
        userService.addTaskResult(userId, task.getId(), passed, taskPayload);
        
        String userStatus = userStatus(userId);
        return new CompleteStepResponse(
                userId, 
                task.getName(), 
                List.of(new TaskResultDto(task.getId(), task.getName(), passed)), 
                userStatus
        );
    }

    /**
     * Finds the step that contains the given task.
     */
    private Step findStepContainingTask(Task targetTask) {
        for (Step step : flow.steps()) {
            for (Task task : step.tasks()) {
                if (task.getId().equals(targetTask.getId())) {
                    return step;
                }
            }
        }
        throw new IllegalStateException("Task not found in any step: " + targetTask.getId());
    }

    /**
     * Validates that all previous tasks in the same step have been completed.
     *
     * @param step the step containing the task
     * @param task the task to validate
     * @param userId the user identifier
     * @throws TaskOrderViolationException if a prerequisite task is not completed
     */
    private void validateTaskOrder(Step step, Task task, String userId) {
        UserStateSnapshot snapshot = userService.snapshot(userId);
        Map<String, TaskResult> completedTasks = snapshot.completedTasks();
        
        // Find the index of the current task in the step
        List<Task> visibleTasks = step.tasks().stream()
                .filter(t -> t.isVisible(snapshot))
                .toList();
        
        int taskIndex = -1;
        for (int i = 0; i < visibleTasks.size(); i++) {
            if (visibleTasks.get(i).getId().equals(task.getId())) {
                taskIndex = i;
                break;
            }
        }
        
        if (taskIndex == -1) {
            throw new IllegalStateException("Task not found in visible tasks of step: " + step.name());
        }
        
        // Check that all previous tasks in the step are completed
        for (int i = 0; i < taskIndex; i++) {
            Task previousTask = visibleTasks.get(i);
            TaskResult result = completedTasks.get(previousTask.getId());
            if (result == null || !result.passed()) {
                throw new TaskOrderViolationException(task.getName(), previousTask.getName());
            }
        }
    }

    /**
     * Finds a task by ID or name (case-insensitive) and validates it's visible for the user.
     *
     * @param taskId the task identifier or name
     * @param userId the user identifier
     * @return the found task
     * @throws TaskNotFoundException if the task is not found or not visible
     */
    private Task findTask(String taskId, String userId) {
        UserStateSnapshot snapshot = userService.snapshot(userId);
        
        for (Step step : flow.steps()) {
            for (Task task : step.tasks()) {
                if ((task.getId().equalsIgnoreCase(taskId) || task.getName().equalsIgnoreCase(taskId))
                        && task.isVisible(snapshot)) {
                    return task;
                }
            }
        }
        
        throw new TaskNotFoundException(taskId);
    }

    /**
     * Validates that all required fields are present in the payload for a task.
     *
     * @param task the task to validate
     * @param payload the payload to validate
     * @throws MissingRequiredFieldsException if any required fields are missing
     */
    private void validateRequiredFields(Task task, Map<String, Object> payload) {
        if (task.getRequiredFields() == null || task.getRequiredFields().isEmpty()) {
            return; // No required fields to validate
        }
        
        if (payload == null) {
            throw new MissingRequiredFieldsException(task.getName(), task.getRequiredFields());
        }
        
        Set<String> missingFields = task.getRequiredFields().stream()
                .filter(field -> !payload.containsKey(field))
                .collect(java.util.stream.Collectors.toSet());
        
        if (!missingFields.isEmpty()) {
            throw new MissingRequiredFieldsException(task.getName(), missingFields);
        }
    }


    /**
     * Gets the current position response for a user, including progress information.
     *
     * @param userId the user identifier
     * @return response containing current position and progress information
     */
    public CurrentPositionResponse getCurrentPositionResponse(String userId) {
        Optional<CurrentPosition> positionOpt = computeCurrentPosition(userId);
        
        if (positionOpt.isEmpty()) {
            return CurrentPositionResponse.completed(userId);
        }

        CurrentPosition position = positionOpt.get();
        List<Step> visibleSteps = getVisibleStepsForUser(userId);
        int totalTasks = visibleSteps.stream()
                .mapToInt(step -> step.tasks().size())
                .sum();
        
        var user = userService.getUser(userId);
        int completedTasks = user != null ? user.getCompletedTasks().size() : 0;

        return new CurrentPositionResponse(
                userId,
                "in_progress",
                position.stepIndex(),
                position.step().id(),
                position.step().name(),
                position.task().getId(),
                position.task().getName(),
                completedTasks,
                totalTasks
        );
    }

    /**
     * Represents the current position of a user in the flow.
     *
     * @param stepIndex 0-based index of the current step
     * @param step      the current step
     * @param task      the current task within the step
     */
    public record CurrentPosition(int stepIndex, Step step, Task task) {
        public CurrentPosition {
            if (stepIndex < 0) {
                throw new IllegalArgumentException("Step index cannot be negative");
            }
            if (step == null) {
                throw new IllegalArgumentException("Step cannot be null");
            }
            if (task == null) {
                throw new IllegalArgumentException("Task cannot be null");
            }
        }
    }
}