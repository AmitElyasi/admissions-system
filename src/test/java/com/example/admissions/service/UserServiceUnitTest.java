package com.example.admissions.service;

import com.example.admissions.exception.DuplicateUserException;
import com.example.admissions.model.TaskResult;
import com.example.admissions.model.User;
import com.example.admissions.model.UserStateSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for UserService covering all methods and edge cases.
 */
class UserServiceUnitTest {
    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService();
    }

    @Test
    void createUser_validEmail_createsUserWithIncrementingId() {
        User user1 = userService.createUser("test1@example.com");
        User user2 = userService.createUser("test2@example.com");
        User user3 = userService.createUser("test3@example.com");

        assertEquals("1", user1.getId());
        assertEquals("2", user2.getId());
        assertEquals("3", user3.getId());
        assertEquals("test1@example.com", user1.getEmail());
        assertEquals("test2@example.com", user2.getEmail());
        assertEquals("test3@example.com", user3.getEmail());
    }

    @Test
    void createUser_duplicateEmail_throwsDuplicateUserException() {
        userService.createUser("duplicate@example.com");
        
        assertThrows(DuplicateUserException.class, () -> {
            userService.createUser("duplicate@example.com");
        });
    }

    @Test
    void createUser_emailCaseInsensitive_throwsDuplicateUserException() {
        userService.createUser("case@example.com");
        
        assertThrows(DuplicateUserException.class, () -> {
            userService.createUser("CASE@example.com");
        });
    }

    @Test
    void createUser_emailWithWhitespace_normalizesForDuplicateCheck() {
        User user1 = userService.createUser("  whitespace@example.com  ");
        
        // User stores original email (with whitespace preserved)
        assertNotNull(user1);
        assertEquals("  whitespace@example.com  ", user1.getEmail());
        
        // But duplicate check uses normalized email, so this should fail
        assertThrows(DuplicateUserException.class, () -> {
            userService.createUser("whitespace@example.com");
        });
        
        // Different case/whitespace but same normalized email should also fail
        assertThrows(DuplicateUserException.class, () -> {
            userService.createUser("  WHITESPACE@example.com  ");
        });
    }

    @Test
    void getUser_existingUser_returnsUser() {
        User created = userService.createUser("get@example.com");
        User retrieved = userService.getUser(created.getId());
        
        assertNotNull(retrieved);
        assertEquals(created.getId(), retrieved.getId());
        assertEquals(created.getEmail(), retrieved.getEmail());
    }

    @Test
    void getUser_nonexistentUser_returnsNull() {
        User retrieved = userService.getUser("99999");
        assertNull(retrieved);
    }

    @Test
    void addTaskResult_existingUser_addsTaskResult() {
        User user = userService.createUser("task@example.com");
        Map<String, Object> payload = Map.of("score", 85, "timestamp", "2025-12-10");
        
        userService.addTaskResult(user.getId(), "task1", true, payload);
        
        Map<String, TaskResult> completedTasks = user.getCompletedTasks();
        assertEquals(1, completedTasks.size());
        assertTrue(completedTasks.containsKey("task1"));
        assertTrue(completedTasks.get("task1").passed());
    }

    @Test
    void addTaskResult_nonexistentUser_doesNotThrow() {
        // Should not throw exception, just silently return
        assertDoesNotThrow(() -> {
            userService.addTaskResult("99999", "task1", true, Map.of());
        });
    }

    @Test
    void addTaskResult_multipleTasks_addsAllTasks() {
        User user = userService.createUser("multitask@example.com");
        
        userService.addTaskResult(user.getId(), "task1", true, Map.of());
        userService.addTaskResult(user.getId(), "task2", false, Map.of());
        userService.addTaskResult(user.getId(), "task3", true, Map.of());
        
        Map<String, TaskResult> completedTasks = user.getCompletedTasks();
        assertEquals(3, completedTasks.size());
        assertTrue(completedTasks.get("task1").passed());
        assertFalse(completedTasks.get("task2").passed());
        assertTrue(completedTasks.get("task3").passed());
    }

    @Test
    void addTaskResult_overwritesExistingTask() {
        User user = userService.createUser("overwrite@example.com");
        
        userService.addTaskResult(user.getId(), "task1", false, Map.of("score", 50));
        userService.addTaskResult(user.getId(), "task1", true, Map.of("score", 90));
        
        Map<String, TaskResult> completedTasks = user.getCompletedTasks();
        assertEquals(1, completedTasks.size());
        assertTrue(completedTasks.get("task1").passed());
    }

    @Test
    void snapshot_existingUser_returnsUserStateSnapshot() {
        User user = userService.createUser("snapshot@example.com");
        userService.addTaskResult(user.getId(), "task1", true, Map.of("score", 85));
        userService.addTaskResult(user.getId(), "task2", false, Map.of("score", 60));
        
        UserStateSnapshot snapshot = userService.snapshot(user.getId());
        
        assertNotNull(snapshot);
        assertEquals(user.getId(), snapshot.userId());
        assertEquals(2, snapshot.completedTasks().size());
        assertTrue(snapshot.completedTasks().containsKey("task1"));
        assertTrue(snapshot.completedTasks().containsKey("task2"));
    }

    @Test
    void snapshot_nonexistentUser_returnsEmptySnapshot() {
        UserStateSnapshot snapshot = userService.snapshot("99999");
        
        assertNotNull(snapshot);
        assertEquals("99999", snapshot.userId());
        assertTrue(snapshot.completedTasks().isEmpty());
    }

    @Test
    void snapshot_userWithNoTasks_returnsEmptyCompletedTasks() {
        User user = userService.createUser("notasks@example.com");
        UserStateSnapshot snapshot = userService.snapshot(user.getId());
        
        assertNotNull(snapshot);
        assertEquals(user.getId(), snapshot.userId());
        assertTrue(snapshot.completedTasks().isEmpty());
    }

    @Test
    void addTaskResult_withTimestampInPayload_usesProvidedTimestamp() {
        User user = userService.createUser("timestamp@example.com");
        String isoTimestamp = "2025-12-10T15:00:00Z";
        Map<String, Object> payload = Map.of("score", 85, "timestamp", isoTimestamp);
        
        userService.addTaskResult(user.getId(), "task1", true, payload);
        
        var taskResult = user.getCompletedTasks().get("task1");
        assertNotNull(taskResult);
        assertEquals(isoTimestamp, taskResult.when().toString());
    }

    @Test
    void addTaskResult_withoutTimestamp_usesCurrentTime() {
        User user = userService.createUser("notimestamp@example.com");
        Map<String, Object> payload = Map.of("score", 85);
        
        long before = System.currentTimeMillis();
        userService.addTaskResult(user.getId(), "task1", true, payload);
        long after = System.currentTimeMillis();
        
        var taskResult = user.getCompletedTasks().get("task1");
        assertNotNull(taskResult);
        long taskTime = taskResult.when().toEpochMilli();
        assertTrue(taskTime >= before && taskTime <= after);
    }

    @Test
    void addTaskResult_withNowTimestamp_usesCurrentTime() {
        User user = userService.createUser("now@example.com");
        Map<String, Object> payload = Map.of("score", 85, "timestamp", "now");
        
        long before = System.currentTimeMillis();
        userService.addTaskResult(user.getId(), "task1", true, payload);
        long after = System.currentTimeMillis();
        
        var taskResult = user.getCompletedTasks().get("task1");
        assertNotNull(taskResult);
        long taskTime = taskResult.when().toEpochMilli();
        assertTrue(taskTime >= before && taskTime <= after);
    }
}

