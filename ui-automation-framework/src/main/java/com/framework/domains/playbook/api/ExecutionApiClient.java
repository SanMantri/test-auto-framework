package com.framework.domains.playbook.api;

import com.framework.core.base.BaseApiClient;
import com.framework.domains.playbook.models.Execution;
import com.framework.domains.playbook.models.Execution.ExecutionStatus;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * ExecutionApiClient - API operations for playbook executions
 *
 * Handles execution lifecycle:
 * - Triggering executions
 * - Monitoring execution status
 * - Retrieving execution results
 * - Cancelling/retrying executions
 */
@Slf4j
@Component
public class ExecutionApiClient extends BaseApiClient {

    @Override
    protected String getBasePath() {
        return "/api/v1/executions";
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // EXECUTION TRIGGERS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Triggers a playbook execution.
     */
    public Execution triggerExecution(String playbookId) {
        log.info("Triggering execution for playbook: {}", playbookId);
        Response response = post("", Map.of("playbookId", playbookId));
        return getCreatedAs(response, Execution.class);
    }

    /**
     * Triggers a playbook execution with variables.
     */
    public Execution triggerExecution(String playbookId, Map<String, Object> variables) {
        log.info("Triggering execution for playbook: {} with variables", playbookId);
        Response response = post("", Map.of(
            "playbookId", playbookId,
            "variables", variables
        ));
        return getCreatedAs(response, Execution.class);
    }

    /**
     * Triggers and waits for completion.
     */
    public Execution triggerAndWait(String playbookId, int timeoutSeconds) {
        Execution execution = triggerExecution(playbookId);
        return waitForCompletion(execution.getId(), timeoutSeconds);
    }

    /**
     * Triggers with variables and waits for completion.
     */
    public Execution triggerAndWait(String playbookId, Map<String, Object> variables, int timeoutSeconds) {
        Execution execution = triggerExecution(playbookId, variables);
        return waitForCompletion(execution.getId(), timeoutSeconds);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // EXECUTION QUERIES
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Gets an execution by ID.
     */
    public Execution getExecution(String executionId) {
        log.info("Getting execution: {}", executionId);
        Response response = get("/" + executionId);
        return getOkAs(response, Execution.class);
    }

    /**
     * Gets executions for a playbook.
     */
    public List<Execution> getExecutionsForPlaybook(String playbookId) {
        log.info("Getting executions for playbook: {}", playbookId);
        Response response = get("", Map.of("playbookId", playbookId));
        return List.of(getOkAs(response, Execution[].class));
    }

    /**
     * Gets recent executions.
     */
    public List<Execution> getRecentExecutions(int limit) {
        log.info("Getting recent {} executions", limit);
        Response response = get("", Map.of(
            "limit", limit,
            "sort", "startedAt:desc"
        ));
        return List.of(getOkAs(response, Execution[].class));
    }

    /**
     * Gets executions with filters.
     */
    public List<Execution> getExecutions(Map<String, Object> filters) {
        log.info("Getting executions with filters: {}", filters);
        Response response = get("", filters);
        return List.of(getOkAs(response, Execution[].class));
    }

    /**
     * Gets execution statistics for a playbook.
     */
    public ExecutionStats getStats(String playbookId) {
        log.info("Getting execution stats for playbook: {}", playbookId);
        Response response = get("/stats", Map.of("playbookId", playbookId));
        return getOkAs(response, ExecutionStats.class);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // EXECUTION CONTROL
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Cancels a running execution.
     */
    public Execution cancelExecution(String executionId) {
        log.info("Cancelling execution: {}", executionId);
        Response response = post("/" + executionId + "/cancel");
        return getOkAs(response, Execution.class);
    }

    /**
     * Pauses a running execution.
     */
    public Execution pauseExecution(String executionId) {
        log.info("Pausing execution: {}", executionId);
        Response response = post("/" + executionId + "/pause");
        return getOkAs(response, Execution.class);
    }

    /**
     * Resumes a paused execution.
     */
    public Execution resumeExecution(String executionId) {
        log.info("Resuming execution: {}", executionId);
        Response response = post("/" + executionId + "/resume");
        return getOkAs(response, Execution.class);
    }

    /**
     * Retries a failed execution.
     */
    public Execution retryExecution(String executionId) {
        log.info("Retrying execution: {}", executionId);
        Response response = post("/" + executionId + "/retry");
        return getCreatedAs(response, Execution.class);
    }

    /**
     * Retries execution from a specific failed step.
     */
    public Execution retryFromStep(String executionId, String stepId) {
        log.info("Retrying execution {} from step: {}", executionId, stepId);
        Response response = post("/" + executionId + "/retry", Map.of(
            "fromStepId", stepId
        ));
        return getCreatedAs(response, Execution.class);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // EXECUTION LOGS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Gets execution logs.
     */
    public List<ExecutionLog> getLogs(String executionId) {
        log.info("Getting logs for execution: {}", executionId);
        Response response = get("/" + executionId + "/logs");
        return List.of(getOkAs(response, ExecutionLog[].class));
    }

    /**
     * Gets logs for a specific step.
     */
    public List<ExecutionLog> getStepLogs(String executionId, String stepId) {
        log.info("Getting logs for step {} in execution: {}", stepId, executionId);
        Response response = get("/" + executionId + "/steps/" + stepId + "/logs");
        return List.of(getOkAs(response, ExecutionLog[].class));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // WAIT UTILITIES
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Waits for execution to complete.
     */
    public Execution waitForCompletion(String executionId, int maxWaitSeconds) {
        log.info("Waiting for execution {} to complete (max {}s)", executionId, maxWaitSeconds);

        long startTime = System.currentTimeMillis();
        long maxWaitMs = maxWaitSeconds * 1000L;

        while (System.currentTimeMillis() - startTime < maxWaitMs) {
            Execution execution = getExecution(executionId);
            if (execution.isComplete()) {
                log.info("Execution completed with status: {}", execution.getStatus());
                return execution;
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while waiting for execution", e);
            }
        }

        throw new RuntimeException(String.format(
            "Execution %s did not complete within %d seconds", executionId, maxWaitSeconds));
    }

    /**
     * Waits for execution to reach a specific status.
     */
    public Execution waitForStatus(String executionId, ExecutionStatus expectedStatus, int maxWaitSeconds) {
        log.info("Waiting for execution {} to reach status: {}", executionId, expectedStatus);

        long startTime = System.currentTimeMillis();
        long maxWaitMs = maxWaitSeconds * 1000L;

        while (System.currentTimeMillis() - startTime < maxWaitMs) {
            Execution execution = getExecution(executionId);
            if (execution.getStatus() == expectedStatus) {
                return execution;
            }
            if (execution.isComplete() && execution.getStatus() != expectedStatus) {
                throw new RuntimeException(String.format(
                    "Execution reached terminal status %s instead of expected %s",
                    execution.getStatus(), expectedStatus));
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while waiting for execution status", e);
            }
        }

        throw new RuntimeException(String.format(
            "Execution %s did not reach status %s within %d seconds",
            executionId, expectedStatus, maxWaitSeconds));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // HELPER CLASSES
    // ═══════════════════════════════════════════════════════════════════════════

    public static class ExecutionStats {
        public int totalExecutions;
        public int successCount;
        public int failureCount;
        public double successRate;
        public double avgDurationMs;
        public long minDurationMs;
        public long maxDurationMs;
    }

    public static class ExecutionLog {
        public String timestamp;
        public String level;  // INFO, WARN, ERROR, DEBUG
        public String stepId;
        public String message;
        public java.util.Map<String, Object> data;
    }
}
