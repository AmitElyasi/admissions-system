package com.example.admissions.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Set;

/**
 * DTOs for JSON deserialization of flow configuration.
 */
public record FlowJsonDto(
        String id,
        String name,
        List<StepJsonDto> steps
) {}

record StepJsonDto(
        String id,
        String name,
        List<TaskJsonDto> tasks
) {}

record TaskJsonDto(
        String id,
        String name,
        @JsonProperty("requiredFields")
        Set<String> requiredFields,
        @JsonProperty("passCondition")
        ConditionJsonDto passCondition,
        @JsonProperty("visibilityCondition")
        ConditionJsonDto visibilityCondition,
        Boolean redoable
) {}

record ConditionJsonDto(
        String type,
        String field,
        String value,
        Double threshold,
        @JsonProperty("taskId")
        String taskId,
        Double min,
        Double max
) {}

