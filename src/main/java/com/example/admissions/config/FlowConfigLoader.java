package com.example.admissions.config;

import com.example.admissions.model.Flow;
import com.example.admissions.model.Step;
import com.example.admissions.model.Task;
import com.example.admissions.model.UserStateSnapshot;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Loads flow configuration from JSON file using Jackson.
 * Enables non-developers (PMs) to modify the flow without code changes.
 */
@Component
public class FlowConfigLoader {
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Loads the flow configuration from flow.json resource file.
     */
    public Flow loadFlow() {
        try {
            ClassPathResource resource = new ClassPathResource("flow.json");
            InputStream inputStream = resource.getInputStream();
            FlowJsonDto flowDto = objectMapper.readValue(inputStream, FlowJsonDto.class);

            List<Step> steps = flowDto.steps().stream()
                    .map(stepDto -> {
                        List<Task> tasks = stepDto.tasks().stream()
                                .map(taskDto -> new Task(
                                        taskDto.id(),
                                        taskDto.name(),
                                        taskDto.requiredFields(),
                                        createPassPredicate(taskDto.passCondition()),
                                        createVisibilityPredicate(taskDto.visibilityCondition()),
                                        taskDto.redoable() != null ? taskDto.redoable() : true // Default to true if not specified
                                ))
                                .toList();
                        return new Step(stepDto.id(), stepDto.name(), tasks);
                    })
                    .toList();

            return new Flow(flowDto.id(), flowDto.name(), steps);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load flow configuration from JSON", e);
        }
    }

    private Predicate<Map<String, Object>> createPassPredicate(ConditionJsonDto condition) {
        return switch (condition.type()) {
            case "always" -> payload -> true;
            case "scoreGreaterThan" -> {
                String field = condition.field();
                double threshold = condition.threshold();
                yield payload -> {
                    Object scoreObj = payload.get(field);
                    if (scoreObj instanceof Number number) {
                        return number.doubleValue() > threshold;
                    }
                    try {
                        return Double.parseDouble(String.valueOf(scoreObj)) > threshold;
                    } catch (Exception ex) {
                        return false;
                    }
                };
            }
            case "equals" -> {
                String field = condition.field();
                String value = condition.value();
                yield payload -> {
                    Object fieldValue = payload.get(field);
                    return fieldValue != null && value.equals(String.valueOf(fieldValue));
                };
            }
            default -> throw new IllegalArgumentException("Unknown pass condition type: " + condition.type());
        };
    }

    private Predicate<UserStateSnapshot> createVisibilityPredicate(ConditionJsonDto condition) {
        return switch (condition.type()) {
            case "always" -> snapshot -> true;
            default -> throw new IllegalArgumentException("Unknown visibility condition type: " + condition.type());
        };
    }
}

