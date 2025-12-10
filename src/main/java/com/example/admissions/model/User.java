package com.example.admissions.model;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory user record: id, email, and completed tasks map.
 */
public class User {
    private final String id;
    private final String email;
    // taskId -> TaskResult
    private final Map<String, TaskResult> completedTasks = new ConcurrentHashMap<>();

    public User(String id, String email) {
        this.id = id;
        this.email = email;
    }

    public String getId() { return id; }
    public String getEmail() { return email; }
    public Map<String, TaskResult> getCompletedTasks() { return completedTasks; }

    public void addTaskResult(TaskResult result) {
        completedTasks.put(result.taskId(), result);
    }
}