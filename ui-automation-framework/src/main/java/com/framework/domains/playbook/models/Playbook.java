package com.framework.domains.playbook.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Playbook - Automation workflow definition model
 *
 * Represents a reusable automation playbook that can be:
 * - Created/edited via UI or API
 * - Executed manually or on schedule
 * - Chained with other playbooks
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Playbook {

    private String id;
    private String name;
    private String description;
    private String version;
    private PlaybookStatus status;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Trigger configuration
    private TriggerConfig trigger;

    // Steps in the playbook
    private List<PlaybookStep> steps;

    // Variables available in the playbook
    private Map<String, Variable> variables;

    // Tags for categorization
    private List<String> tags;

    // Permissions
    private List<String> sharedWith;
    private boolean isPublic;

    // Execution settings
    private ExecutionSettings executionSettings;

    public enum PlaybookStatus {
        DRAFT,
        ACTIVE,
        PAUSED,
        ARCHIVED,
        ERROR
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TriggerConfig {
        private TriggerType type;
        private String cronExpression;  // For scheduled triggers
        private String webhookUrl;       // For webhook triggers
        private Map<String, Object> eventFilters;  // For event triggers
    }

    public enum TriggerType {
        MANUAL,
        SCHEDULED,
        WEBHOOK,
        EVENT
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlaybookStep {
        private String id;
        private String name;
        private StepType type;
        private int order;
        private Map<String, Object> config;
        private List<String> dependsOn;  // Step IDs this step depends on
        private ConditionalConfig condition;
        private ErrorHandling errorHandling;
    }

    public enum StepType {
        HTTP_REQUEST,
        DATABASE_QUERY,
        SEND_EMAIL,
        SEND_SLACK,
        TRANSFORM_DATA,
        WAIT,
        CONDITIONAL,
        LOOP,
        SCRIPT,
        SUB_PLAYBOOK
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Variable {
        private String name;
        private VariableType type;
        private Object defaultValue;
        private boolean required;
        private boolean sensitive;  // For secrets
    }

    public enum VariableType {
        STRING,
        NUMBER,
        BOOLEAN,
        JSON,
        SECRET
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConditionalConfig {
        private String expression;  // e.g., "{{step1.status}} == 'success'"
        private String onTrue;      // Step ID to execute if true
        private String onFalse;     // Step ID to execute if false
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorHandling {
        private ErrorAction action;
        private int retryCount;
        private int retryDelaySeconds;
        private String fallbackStepId;
    }

    public enum ErrorAction {
        FAIL,
        CONTINUE,
        RETRY,
        FALLBACK
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExecutionSettings {
        private int timeoutSeconds;
        private boolean parallelExecution;
        private int maxConcurrentSteps;
        private boolean logSensitiveData;
    }

    // Helper methods
    public boolean isActive() {
        return status == PlaybookStatus.ACTIVE;
    }

    public boolean isDraft() {
        return status == PlaybookStatus.DRAFT;
    }

    public int getStepCount() {
        return steps != null ? steps.size() : 0;
    }

    public PlaybookStep getStep(String stepId) {
        if (steps == null) return null;
        return steps.stream()
            .filter(s -> s.getId().equals(stepId))
            .findFirst()
            .orElse(null);
    }
}
