package com.example.admissions.config;

import com.example.admissions.model.Flow;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration that loads the flow from JSON file.
 * 
 * <h3>Modifying the Flow</h3>
 * <p>The flow is now defined in <code>src/main/resources/flow.json</code>.</p>
 * <p>Non-developers (PMs) can easily modify the flow by editing the JSON file:</p>
 * <ul>
 *   <li><b>Add a step:</b> Add a new step object to the "steps" array</li>
 *   <li><b>Remove a step:</b> Remove the step object from the "steps" array</li>
 *   <li><b>Reorder steps:</b> Change the order in the "steps" array - order matters!</li>
 *   <li><b>Add a task:</b> Add a new task object to a step's "tasks" array</li>
 *   <li><b>Remove a task:</b> Remove the task object from the step's "tasks" array</li>
 *   <li><b>Reorder tasks:</b> Change the order in the step's "tasks" array</li>
 *   <li><b>Modify conditions:</b> Update the passCondition or visibilityCondition</li>
 * </ul>
 * 
 * <h3>Supported Pass Conditions</h3>
 * <ul>
 *   <li><code>{"type": "always"}</code> - Task always passes when completed</li>
 *   <li><code>{"type": "scoreGreaterThan", "field": "score", "threshold": 75.0}</code> - Passes if score > threshold</li>
 *   <li><code>{"type": "equals", "field": "decision", "value": "passed_interview"}</code> - Passes if field equals value</li>
 * </ul>
 * 
 * <h3>Supported Visibility Conditions</h3>
 * <ul>
 *   <li><code>{"type": "always"}</code> - Task always visible</li>
 *   <li><code>{"type": "previousTaskScoreBetween", "taskId": "iq_test", "min": 60.0, "max": 75.0}</code> - Visible if previous task score is between min and max</li>
 * </ul>
 */
@Configuration
public class FlowConfig {
    private final FlowConfigLoader flowConfigLoader;

    public FlowConfig(FlowConfigLoader flowConfigLoader) {
        this.flowConfigLoader = flowConfigLoader;
    }

    @Bean
    public Flow admissionsFlow() {
        return flowConfigLoader.loadFlow();
    }
}
