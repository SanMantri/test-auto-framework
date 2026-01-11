package com.framework.domains.playbook.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Execution - Playbook execution instance
 *
 * Represents a single execution of a playbook with:
 * - Execution status and timing
 * - Step-by-step results
 * - Input/output data
 * - Error information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Execution {

    private String id;
    private String playbookId;
    private String playbookName;
    private String playbookVersion;
    private ExecutionStatus status;

    // Timing
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private Long durationMs;

    // Trigger info
    private TriggerInfo trigger;

    // Input variables provided at execution time
    private Map<String, Object> inputVariables;

    // Output data from execution
    private Map<String, Object> outputData;

    // Step results
    private List<StepResult> stepResults;

    // Error info if failed
    private ErrorInfo error;

    // Who/what initiated this execution
    private String initiatedBy;

    // Retry info
    private int attemptNumber;
    private String parentExecutionId;  // If this is a retry

    public enum ExecutionStatus {
        QUEUED,
        RUNNING,
        SUCCESS,
        FAILED,
        CANCELLED,
        TIMEOUT,
        PAUSED
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TriggerInfo {
        private Playbook.TriggerType type;
        private String triggeredBy;  // User ID, webhook ID, schedule name
        private Map<String, Object> triggerData;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StepResult {
        private String stepId;
        private String stepName;
        private Playbook.StepType stepType;
        private StepStatus status;
        private LocalDateTime startedAt;
        private LocalDateTime completedAt;
        private Long durationMs;
        private Map<String, Object> input;
        private Map<String, Object> output;
        private String errorMessage;
        private int retryCount;
    }

    public enum StepStatus {
        PENDING,
        RUNNING,
        SUCCESS,
        FAILED,
        SKIPPED,
        CANCELLED
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorInfo {
        private String stepId;
        private String errorCode;
        private String errorMessage;
        private String stackTrace;
        private Map<String, Object> context;
    }

    // Helper methods
    public boolean isSuccessful() {
        return status == ExecutionStatus.SUCCESS;
    }

    public boolean isFailed() {
        return status == ExecutionStatus.FAILED;
    }

    public boolean isComplete() {
        return status == ExecutionStatus.SUCCESS ||
               status == ExecutionStatus.FAILED ||
               status == ExecutionStatus.CANCELLED ||
               status == ExecutionStatus.TIMEOUT;
    }

    public boolean isRunning() {
        return status == ExecutionStatus.RUNNING;
    }

    public Duration getDuration() {
        if (durationMs != null) {
            return Duration.ofMillis(durationMs);
        }
        if (startedAt != null && completedAt != null) {
            return Duration.between(startedAt, completedAt);
        }
        return null;
    }

    public int getSuccessfulStepCount() {
        if (stepResults == null) return 0;
        return (int) stepResults.stream()
            .filter(s -> s.getStatus() == StepStatus.SUCCESS)
            .count();
    }

    public int getFailedStepCount() {
        if (stepResults == null) return 0;
        return (int) stepResults.stream()
            .filter(s -> s.getStatus() == StepStatus.FAILED)
            .count();
    }

    public StepResult getStepResult(String stepId) {
        if (stepResults == null) return null;
        return stepResults.stream()
            .filter(s -> s.getStepId().equals(stepId))
            .findFirst()
            .orElse(null);
    }

    public StepResult getFailedStep() {
        if (stepResults == null) return null;
        return stepResults.stream()
            .filter(s -> s.getStatus() == StepStatus.FAILED)
            .findFirst()
            .orElse(null);
    }
}
