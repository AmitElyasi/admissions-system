package com.example.admissions.controller;

import com.example.admissions.dto.CompleteStepRequest;
import com.example.admissions.dto.CompleteStepResponse;
import com.example.admissions.service.FlowService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for step completion operations.
 * Handles marking steps/tasks as completed by matching payloads to tasks.
 */
@RestController
@RequestMapping("/steps")
public class StepController {
    private final FlowService flowService;

    public StepController(FlowService flowService) {
        this.flowService = flowService;
    }

    /**
     * Marks a task as completed.
     * Validates that all required fields are present and evaluates the pass condition.
     *
     * @param request the task completion request
     * @return response containing task result and updated user status
     */
    @PutMapping("/complete")
    public ResponseEntity<CompleteStepResponse> completeStep(@Valid @RequestBody CompleteStepRequest request) {
        return ResponseEntity.ok(flowService.completeStep(
                request.userId(),
                request.taskId(),
                request.taskPayload()
        ));
    }
}