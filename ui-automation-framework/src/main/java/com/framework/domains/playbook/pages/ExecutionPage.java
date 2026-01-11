package com.framework.domains.playbook.pages;

import com.framework.core.base.BasePage;
import com.framework.domains.playbook.models.Execution.ExecutionStatus;
import com.microsoft.playwright.Page;
import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * ExecutionPage - Playbook execution monitoring page object
 *
 * Handles viewing and controlling playbook executions:
 * - Real-time execution status
 * - Step-by-step progress
 * - Logs and outputs
 * - Execution control (cancel, retry)
 */
@Slf4j
public class ExecutionPage extends BasePage {

    // ═══════════════════════════════════════════════════════════════════════════
    // LOCATORS
    // ═══════════════════════════════════════════════════════════════════════════

    // Execution Header
    private static final String EXECUTION_ID = "[data-testid='execution-id']";
    private static final String EXECUTION_STATUS = "[data-testid='execution-status']";
    private static final String PLAYBOOK_NAME = "[data-testid='playbook-name']";
    private static final String START_TIME = "[data-testid='start-time']";
    private static final String DURATION = "[data-testid='duration']";

    // Progress
    private static final String PROGRESS_BAR = "[data-testid='progress-bar']";
    private static final String STEP_PROGRESS = "[data-testid='step-progress']"; // e.g., "3/5 steps completed"

    // Steps List
    private static final String STEP_LIST = "[data-testid='step-list']";
    private static final String STEP_ITEM = "[data-testid='step-%s']";
    private static final String STEP_STATUS_ICON = "[data-testid='step-%s-status']";
    private static final String STEP_DURATION = "[data-testid='step-%s-duration']";
    private static final String STEP_EXPAND = "[data-testid='step-%s-expand']";

    // Step Details (when expanded)
    private static final String STEP_INPUT = "[data-testid='step-input']";
    private static final String STEP_OUTPUT = "[data-testid='step-output']";
    private static final String STEP_ERROR = "[data-testid='step-error']";

    // Logs
    private static final String LOGS_TAB = "[data-testid='logs-tab']";
    private static final String LOG_ENTRIES = "[data-testid='log-entries']";
    private static final String LOG_ENTRY = "[data-testid='log-entry']";
    private static final String LOG_FILTER = "[data-testid='log-filter']";

    // Actions
    private static final String CANCEL_BUTTON = "[data-testid='cancel-execution']";
    private static final String RETRY_BUTTON = "[data-testid='retry-execution']";
    private static final String DOWNLOAD_LOGS_BUTTON = "[data-testid='download-logs']";
    private static final String BACK_BUTTON = "[data-testid='back-to-playbook']";

    // Output/Results
    private static final String OUTPUT_TAB = "[data-testid='output-tab']";
    private static final String OUTPUT_DATA = "[data-testid='output-data']";

    // Error Display
    private static final String ERROR_BANNER = "[data-testid='error-banner']";
    private static final String ERROR_MESSAGE = "[data-testid='error-message']";
    private static final String ERROR_STEP = "[data-testid='error-step']";

    // ═══════════════════════════════════════════════════════════════════════════
    // CONSTRUCTOR & NAVIGATION
    // ═══════════════════════════════════════════════════════════════════════════

    public ExecutionPage(Page page) {
        super(page);
    }

    @Step("Navigate to execution: {executionId}")
    public ExecutionPage navigate(String executionId) {
        navigateTo("/executions/" + executionId);
        return this;
    }

    @Override
    protected void waitForPageLoad() {
        waitForVisible(EXECUTION_STATUS);
    }

    @Override
    public boolean isDisplayed() {
        return isVisible(EXECUTION_STATUS);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // EXECUTION INFO
    // ═══════════════════════════════════════════════════════════════════════════

    public String getExecutionId() {
        return getText(EXECUTION_ID);
    }

    public String getStatus() {
        return getText(EXECUTION_STATUS);
    }

    public ExecutionStatus getExecutionStatus() {
        String status = getStatus().toUpperCase().replace(" ", "_");
        return ExecutionStatus.valueOf(status);
    }

    public String getPlaybookName() {
        return getText(PLAYBOOK_NAME);
    }

    public String getStartTime() {
        return getText(START_TIME);
    }

    public String getDuration() {
        return getText(DURATION);
    }

    public String getStepProgress() {
        return getText(STEP_PROGRESS);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // STEP DETAILS
    // ═══════════════════════════════════════════════════════════════════════════

    @Step("Expand step details: {stepId}")
    public ExecutionPage expandStep(String stepId) {
        log.info("Expanding step: {}", stepId);
        String locator = String.format(STEP_EXPAND, stepId);
        click(locator);
        return this;
    }

    public String getStepStatus(String stepId) {
        String locator = String.format(STEP_STATUS_ICON, stepId);
        return getAttribute(locator, "data-status");
    }

    public String getStepDuration(String stepId) {
        String locator = String.format(STEP_DURATION, stepId);
        return getText(locator);
    }

    public String getStepInput() {
        return getText(STEP_INPUT);
    }

    public String getStepOutput() {
        return getText(STEP_OUTPUT);
    }

    public String getStepError() {
        if (isVisible(STEP_ERROR)) {
            return getText(STEP_ERROR);
        }
        return null;
    }

    public List<String> getStepIds() {
        return page.locator(STEP_LIST + " [data-testid^='step-']").all().stream()
            .map(l -> {
                String testId = l.getAttribute("data-testid");
                return testId.replace("step-", "").split("-")[0];
            })
            .distinct()
            .toList();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // LOGS
    // ═══════════════════════════════════════════════════════════════════════════

    @Step("Open logs tab")
    public ExecutionPage openLogsTab() {
        click(LOGS_TAB);
        waitForVisible(LOG_ENTRIES);
        return this;
    }

    @Step("Filter logs by level: {level}")
    public ExecutionPage filterLogs(String level) {
        log.info("Filtering logs by: {}", level);
        selectOption(LOG_FILTER, level);
        return this;
    }

    public int getLogCount() {
        return page.locator(LOG_ENTRY).count();
    }

    public List<String> getLogMessages() {
        return page.locator(LOG_ENTRY).all().stream()
            .map(l -> l.textContent())
            .toList();
    }

    @Step("Download logs")
    public ExecutionPage downloadLogs() {
        click(DOWNLOAD_LOGS_BUTTON);
        return this;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // OUTPUT
    // ═══════════════════════════════════════════════════════════════════════════

    @Step("Open output tab")
    public ExecutionPage openOutputTab() {
        click(OUTPUT_TAB);
        waitForVisible(OUTPUT_DATA);
        return this;
    }

    public String getOutputData() {
        return getText(OUTPUT_DATA);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ERROR HANDLING
    // ═══════════════════════════════════════════════════════════════════════════

    public boolean hasError() {
        return isVisible(ERROR_BANNER);
    }

    public String getErrorMessage() {
        if (hasError()) {
            return getText(ERROR_MESSAGE);
        }
        return null;
    }

    public String getErrorStep() {
        if (hasError()) {
            return getText(ERROR_STEP);
        }
        return null;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ACTIONS
    // ═══════════════════════════════════════════════════════════════════════════

    @Step("Cancel execution")
    public ExecutionPage cancelExecution() {
        log.info("Cancelling execution");
        click(CANCEL_BUTTON);
        // Wait for status to update
        waitFor(1000);
        return this;
    }

    @Step("Retry execution")
    public ExecutionPage retryExecution() {
        log.info("Retrying execution");
        click(RETRY_BUTTON);
        // Wait for new execution to start
        waitForVisible(EXECUTION_STATUS);
        return this;
    }

    @Step("Go back to playbook")
    public PlaybookBuilderPage goBackToPlaybook() {
        click(BACK_BUTTON);
        return new PlaybookBuilderPage(page);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // WAIT UTILITIES
    // ═══════════════════════════════════════════════════════════════════════════

    @Step("Wait for execution to complete")
    public ExecutionPage waitForCompletion(int maxWaitSeconds) {
        log.info("Waiting for execution to complete (max {}s)", maxWaitSeconds);

        long startTime = System.currentTimeMillis();
        long maxWaitMs = maxWaitSeconds * 1000L;

        while (System.currentTimeMillis() - startTime < maxWaitMs) {
            String status = getStatus().toUpperCase();
            if (status.contains("SUCCESS") || status.contains("FAILED") ||
                status.contains("CANCELLED") || status.contains("TIMEOUT")) {
                log.info("Execution completed with status: {}", status);
                return this;
            }

            waitFor(1000);
            page.reload(); // Refresh to get latest status
        }

        throw new RuntimeException("Execution did not complete within " + maxWaitSeconds + " seconds");
    }

    @Step("Wait for execution to succeed")
    public ExecutionPage waitForSuccess(int maxWaitSeconds) {
        waitForCompletion(maxWaitSeconds);

        if (!getStatus().toUpperCase().contains("SUCCESS")) {
            throw new RuntimeException("Execution did not succeed. Status: " + getStatus());
        }

        return this;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ASSERTIONS
    // ═══════════════════════════════════════════════════════════════════════════

    public boolean isRunning() {
        return getStatus().toUpperCase().contains("RUNNING");
    }

    public boolean isSuccessful() {
        return getStatus().toUpperCase().contains("SUCCESS");
    }

    public boolean isFailed() {
        return getStatus().toUpperCase().contains("FAILED");
    }

    public boolean isCancelled() {
        return getStatus().toUpperCase().contains("CANCELLED");
    }
}
