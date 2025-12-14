package com.example.admissions.model;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory user record: id, email, completed tasks map, and cached final status.
 */
public class User {
    private final String id;
    private final String email;
    // taskId -> TaskResult
    private final Map<String, TaskResult> completedTasks = new ConcurrentHashMap<>();
    // Cached final status: "accepted" or "rejected" (null if still in_progress)
    private volatile String finalStatus;

    public User(String id, String email) {
        this.id = id;
        this.email = email;
        this.finalStatus = null;
    }

    public String getId() { return id; }
    public String getEmail() { return email; }
    public Map<String, TaskResult> getCompletedTasks() { return completedTasks; }
    public String getFinalStatus() { return finalStatus; }

    public void addTaskResult(TaskResult result) {
        completedTasks.put(result.taskId(), result);
    }

    /**
     * Sets the final status if it's "accepted" or "rejected".
     * Once set, the status is final and won't change.
     */
    public void setFinalStatus(String status) {
        if ("accepted".equals(status) || "rejected".equals(status)) {
            this.finalStatus = status;
        }
    }
}