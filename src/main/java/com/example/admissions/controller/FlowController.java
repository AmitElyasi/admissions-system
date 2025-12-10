package com.example.admissions.controller;

import com.example.admissions.model.Flow;
import com.example.admissions.service.FlowService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Exposes flow template for frontend (alternate endpoint).
 */
@RestController
@RequestMapping("/flow")
public class FlowController {
    private final FlowService flowService;

    public FlowController(FlowService flowService) {
        this.flowService = flowService;
    }

    @GetMapping
    public ResponseEntity<Flow> getFlow() {
        return ResponseEntity.ok(flowService.getFlow());
    }
}