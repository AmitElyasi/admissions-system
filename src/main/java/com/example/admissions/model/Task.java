package com.example.admissions.model;

import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Task definition inside the flow.
 * - id: unique key for the task (e.g. "iq_test")
 * - name: human readable name
 * - requiredFields: set of fields to detect incoming payloads for this task
 * - passPredicate: given received payload, decide whether task passed
 * - visibleFor: predicate to decide if task is visible for a particular user based on their state
 * - redoable: whether the task can be completed multiple times (default: true)
 */
public class Task {
    private final String id;
    private final String name;
    private final Set<String> requiredFields;
    private final Predicate<Map<String, Object>> passPredicate;
    private final Predicate<UserStateSnapshot> visibleFor;
    private final boolean redoable;

    public Task(String id, String name, Set<String> requiredFields,
                Predicate<Map<String,Object>> passPredicate,
                Predicate<UserStateSnapshot> visibleFor,
                boolean redoable) {
        this.id = id;
        this.name = name;
        this.requiredFields = requiredFields;
        this.passPredicate = passPredicate;
        this.visibleFor = visibleFor;
        this.redoable = redoable;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public Set<String> getRequiredFields() { return requiredFields; }
    public boolean isRedoable() { return redoable; }

    public boolean evaluatePassed(Map<String,Object> payload) {
        if (passPredicate == null) return true; // default pass when payload arrives
        return passPredicate.test(payload);
    }

    public boolean isVisible(UserStateSnapshot snapshot) {
        if (visibleFor == null) return true;
        return visibleFor.test(snapshot);
    }
}