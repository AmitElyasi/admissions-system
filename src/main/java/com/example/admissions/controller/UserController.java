package com.example.admissions.controller;

import com.example.admissions.dto.CreateUserRequest;
import com.example.admissions.dto.CreateUserResponse;
import com.example.admissions.dto.CurrentPositionResponse;
import com.example.admissions.dto.UserStatusResponse;
import com.example.admissions.service.FlowService;
import com.example.admissions.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for user-related operations.
 * Handles user creation, flow retrieval, current position, and status checks.
 */
@RestController
@RequestMapping("/users")
public class UserController {
    private final UserService userService;
    private final FlowService flowService;

    public UserController(UserService userService, FlowService flowService) {
        this.userService = userService;
        this.flowService = flowService;
    }

    /**
     * Creates a new user in the system.
     *
     * @param request the user creation request containing email
     * @return response containing the created user's ID
     */
    @PostMapping
    public ResponseEntity<CreateUserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        var user = userService.createUser(request.email());
        return ResponseEntity.ok(new CreateUserResponse(user.getId()));
    }

    /**
     * Fetches the current step and task for a specific user.
     * Returns detailed information about the user's position in the flow.
     *
     * @param userId the user identifier
     * @return response containing current position and progress information
     */
    @GetMapping("/{userId}/current")
    public ResponseEntity<CurrentPositionResponse> getCurrentPosition(@PathVariable String userId) {
        return ResponseEntity.ok(flowService.getCurrentPositionResponse(userId));
    }

    /**
     * Checks whether a user is accepted, rejected, or still in progress.
     *
     * @param userId the user identifier
     * @return response containing the user's status
     */
    @GetMapping("/{userId}/status")
    public ResponseEntity<UserStatusResponse> getUserStatus(@PathVariable String userId) {
        return ResponseEntity.ok(flowService.getUserStatusResponse(userId));
    }
}