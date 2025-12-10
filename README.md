# Admissions Flow System

A Spring Boot application implementing a flexible, configurable admissions flow system.

## Features

- Configurable multi-step admissions flow
- User progress tracking
- Task order enforcement within steps
- Required field validation
- Conditional task visibility

## Architecture

- **Models**: Immutable records (Flow, Step, TaskResult, UserStateSnapshot) and classes (Task, User)
- **Services**: FlowService (flow logic), UserService (user management)
- **Controllers**: Thin REST endpoints - only handle HTTP concerns, delegate to services
- **Configuration**: JSON-based flow definition (`flow.json`) loaded by `FlowConfigLoader`
- **DTOs**: Type-safe request/response objects with validation
- **Exceptions**: Custom exceptions with global exception handler

## API Endpoints

1. **POST** `/users` - Create a new user
   ```json
   { "email": "user@example.com" }
   ```

2. **GET** `/flow` - Retrieve the complete flow structure

3. **GET** `/users/{userId}/current` - Get current step and task for a user

4. **PUT** `/steps/complete` - Mark a task as completed
   ```json
   {
     "user_id": "user-id",
     "task_id": "iq_test",
     "task_payload": { "user_id": "...", "test_id": "...", "score": 85, "timestamp": "..." }
   }
   ```
   **Note:** Tasks within a step must be completed in order. For example, you cannot complete "perform_interview" before "schedule_interview".

5. **GET** `/users/{userId}/status` - Check user status (accepted/rejected/in_progress)


## Prerequisites

- Java 21 or higher
- Maven 3.6+

## Running the Application

```bash
# Run tests
mvn test

# Build and run
mvn spring-boot:run

# Package
mvn clean package
```

## Testing

The codebase includes unit tests and integration tests covering all services and API endpoints. A Postman collection (`Admissions_API.postman_collection.json`) is available for API testing.


## Modifying the Flow

The flow is now defined in **`src/main/resources/flow.json`**.

### Adding a New Step

Add a new step object to the `"steps"` array in `flow.json`:

```json
{
  "id": "step_orientation",
  "name": "Orientation",
  "tasks": [
    {
      "id": "attend_orientation",
      "name": "Attend Orientation",
      "requiredFields": ["user_id", "attendance_date", "timestamp"],
      "passCondition": { "type": "always" },
      "visibilityCondition": { "type": "always" }
    }
  ]
}
```

### Removing a Step

Remove the step object from the `"steps"` array in `flow.json`.

### Reordering Steps

Change the order in the `"steps"` array - the order determines the flow sequence.

### Adding a Task to an Existing Step

Add a new task object to a step's `"tasks"` array. **Important:** Tasks are completed in the order they appear in the array. All previous tasks in the same step must be completed before a task can be completed.

```json
{
  "id": "retake_interview",
  "name": "Retake Interview",
  "requiredFields": ["user_id", "interview_date", "decision"],
  "passCondition": {
    "type": "equals",
    "field": "decision",
    "value": "passed_interview"
  },
  "visibilityCondition": { "type": "always" },
  "redoable": true
}
```

**Task Properties:**
- `redoable` (optional, default: `true`): Whether the task can be completed multiple times. Set to `false` to prevent redoing a completed task (e.g., IQ test, payment).

#### Supported Pass Conditions

- `{"type": "always"}` - Task always passes when completed
- `{"type": "scoreGreaterThan", "field": "score", "threshold": 75.0}` - Passes if score > threshold
- `{"type": "equals", "field": "decision", "value": "passed_interview"}` - Passes if field equals value

#### Supported Visibility Conditions

- `{"type": "always"}` - Task always visible

**Note:** The visibility system can be extended in the future to support conditional tasks. For example, you could add a `previousTaskScoreBetween` condition that shows a task only when a previous task's score falls within a range:

```json
{
  "type": "previousTaskScoreBetween",
  "taskId": "iq_test",
  "min": 60.0,
  "max": 75.0
}
```

This would require adding the condition handler in `FlowConfigLoader.createVisibilityPredicate()`.

## Task Completion Rules

1. **Required Fields**: All fields listed in `requiredFields` must be present in the payload
2. **Task Order**: Tasks within a step must be completed in order. All previous tasks in the same step must be completed and passed before a task can be completed
3. **Redoable Configuration**: Tasks can be configured as redoable or not via the `redoable` field in JSON (default: `true`). If `redoable: false`, the task cannot be completed again once completed. Currently, IQ Test, Schedule Interview, Perform Interview, and Payment are non-redoable
4. **Pass Conditions**: Tasks are evaluated against their `passCondition`. If no condition is specified, the task passes by default when completed
5. **Visibility**: Tasks are only visible if their `visibilityCondition` evaluates to true based on the user's current state
