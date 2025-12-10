package com.example.admissions.dto;

/**
 * Response DTO for user status.
 *
 * @param status the user's status: "accepted", "rejected", or "in_progress"
 */
public record UserStatusResponse(String status) {
}

