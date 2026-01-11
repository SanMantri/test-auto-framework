package com.framework.tests.playbook;

import com.framework.core.base.BaseTest;
import com.framework.core.data.TestDataCache;
import com.framework.domains.playbook.api.ExecutionApiClient;
import com.framework.domains.playbook.api.PlaybookApiClient;
import com.framework.domains.playbook.mocks.MockSlackServer;
import com.framework.domains.playbook.models.Execution;
import com.framework.domains.playbook.models.Playbook;
import com.framework.domains.playbook.models.Playbook.StepType;
import com.framework.domains.playbook.pages.ExecutionPage;
import com.framework.domains.playbook.pages.PlaybookBuilderPage;
import io.qameta.allure.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.*;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PlaybookExecutionTest - Tests for playbook creation and execution
 *
 * Tests the workflow automation system including:
 * - Playbook creation via UI
 * - Execution triggering and monitoring
 * - Step execution verification
 * - Integration testing with mocks
 */
@Epic("Workflow Automation")
@Feature("Playbook Execution")
public class PlaybookExecutionTest extends BaseTest {

    @Autowired
    private PlaybookApiClient playbookApi;

    @Autowired
    private ExecutionApiClient executionApi;

    private PlaybookBuilderPage builderPage;
    private ExecutionPage executionPage;
    private TestDataCache testData;

    private MockSlackServer mockSlack;

    @BeforeClass
    public void setupMocks() throws Exception {
        // Start mock Slack server for integration tests
        mockSlack = MockSlackServer.createAndStart(8089);
    }

    @AfterClass
    public void teardownMocks() {
        if (mockSlack != null) {
            mockSlack.stop();
        }
    }

    @BeforeMethod
    public void setupTest() {
        testData = getTestData();
        builderPage = new PlaybookBuilderPage(getPage());
        executionPage = new ExecutionPage(getPage());

        // Reset mock for each test
        if (mockSlack != null) {
            mockSlack.reset();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PLAYBOOK CREATION TESTS
    // ═══════════════════════════════════════════════════════════════════════════

    @Test(description = "Create simple HTTP playbook via UI")
    @Story("Playbook Creation")
    @Severity(SeverityLevel.CRITICAL)
    public void testCreateHttpPlaybook() {
        // Navigate to builder
        builderPage.navigateToCreate();

        // Create playbook
        builderPage.setPlaybookName("Test HTTP Playbook");
        builderPage.setDescription("Automated HTTP request playbook");

        // Add HTTP step
        builderPage.addStep(StepType.HTTP_REQUEST);
        builderPage.setStepName("Call API");
        builderPage.configureHttpGet("https://api.example.com/health");
        builderPage.saveStep();

        // Save playbook
        builderPage.savePlaybook();

        // Verify saved
        assertThat(builderPage.isSaved())
            .as("Playbook should be saved successfully")
            .isTrue();

        // Verify via API
        List<Playbook> playbooks = playbookApi.getMyPlaybooks();
        assertThat(playbooks)
            .extracting(Playbook::getName)
            .contains("Test HTTP Playbook");
    }

    @Test(description = "Create playbook with Slack notification")
    @Story("Playbook Creation")
    @Severity(SeverityLevel.NORMAL)
    public void testCreateSlackNotificationPlaybook() {
        builderPage.navigateToCreate();

        builderPage.setPlaybookName("Slack Notification Playbook");

        // Add Slack step with mock webhook
        builderPage.addStep(StepType.SEND_SLACK);
        builderPage.setStepName("Notify Team");
        builderPage.configureSlackStep("#alerts", "Test notification");
        builderPage.setSlackWebhook(mockSlack.getWebhookUrl());
        builderPage.saveStep();

        builderPage.savePlaybook();

        assertThat(builderPage.isSaved()).isTrue();
        assertThat(builderPage.getStepCount()).isEqualTo(1);
    }

    @Test(description = "Create multi-step playbook")
    @Story("Playbook Creation")
    @Severity(SeverityLevel.CRITICAL)
    public void testCreateMultiStepPlaybook() {
        builderPage.navigateToCreate();

        builderPage.setPlaybookName("Multi-Step Workflow");
        builderPage.setDescription("Complex workflow with multiple steps");

        // Step 1: HTTP Request
        builderPage.addStep(StepType.HTTP_REQUEST);
        builderPage.setStepName("Fetch Data");
        builderPage.configureHttpGet("https://api.example.com/data");
        builderPage.saveStep();

        // Step 2: Wait
        builderPage.addStep(StepType.WAIT);
        builderPage.setStepName("Pause");
        builderPage.configureWaitStep(5, "seconds");
        builderPage.saveStep();

        // Step 3: Send notification
        builderPage.addStep(StepType.SEND_SLACK);
        builderPage.setStepName("Notify");
        builderPage.configureSlackStep("#general", "Data fetched successfully");
        builderPage.saveStep();

        builderPage.savePlaybook();

        assertThat(builderPage.getStepCount()).isEqualTo(3);
        assertThat(builderPage.isSaved()).isTrue();
    }

    @Test(description = "Create playbook with variables")
    @Story("Playbook Creation")
    @Severity(SeverityLevel.NORMAL)
    public void testCreatePlaybookWithVariables() {
        builderPage.navigateToCreate();

        builderPage.setPlaybookName("Parameterized Playbook");

        // Add variables
        builderPage.openVariablesTab();
        builderPage.addVariable("api_url", "STRING", "https://api.example.com", true);
        builderPage.addVariable("timeout", "NUMBER", "30", false);

        // Add step using variable
        builderPage.addStep(StepType.HTTP_REQUEST);
        builderPage.setStepName("Call API");
        builderPage.configureHttpGet("{{api_url}}/health");
        builderPage.saveStep();

        builderPage.savePlaybook();

        assertThat(builderPage.isSaved()).isTrue();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PLAYBOOK EXECUTION TESTS
    // ═══════════════════════════════════════════════════════════════════════════

    @Test(description = "Execute playbook via UI and monitor")
    @Story("Playbook Execution")
    @Severity(SeverityLevel.CRITICAL)
    public void testExecutePlaybookViaUI() {
        // Setup: Create playbook via API
        Playbook playbook = createTestPlaybook("UI Execution Test");
        testData.set("playbookId", playbook.getId());

        // Edit and trigger via UI
        builderPage.navigateToEdit(playbook.getId());
        ExecutionPage execPage = builderPage.triggerTestRun();

        // Wait for completion
        execPage.waitForCompletion(60);

        // Verify
        assertThat(execPage.isSuccessful())
            .as("Execution should be successful")
            .isTrue();
    }

    @Test(description = "Execute playbook via API and verify steps")
    @Story("Playbook Execution")
    @Severity(SeverityLevel.CRITICAL)
    public void testExecutePlaybookViaAPI() {
        // Setup: Create playbook
        Playbook playbook = createTestPlaybook("API Execution Test");
        testData.set("playbookId", playbook.getId());

        // Execute via API
        Execution execution = executionApi.triggerAndWait(playbook.getId(), 60);

        // Verify
        assertThat(execution.isSuccessful()).isTrue();
        assertThat(execution.getStepResults()).isNotEmpty();
        assertThat(execution.getSuccessfulStepCount())
            .isEqualTo(execution.getStepResults().size());

        // Verify in UI
        executionPage.navigate(execution.getId());
        assertThat(executionPage.isSuccessful()).isTrue();
    }

    @Test(description = "Execute playbook with variables")
    @Story("Playbook Execution")
    @Severity(SeverityLevel.NORMAL)
    public void testExecutePlaybookWithVariables() {
        // Setup: Create parameterized playbook
        Playbook playbook = createParameterizedPlaybook("Variable Test");
        testData.set("playbookId", playbook.getId());

        // Execute with variables
        Map<String, Object> variables = Map.of(
            "target_url", "https://api.example.com/test",
            "message", "Custom test message"
        );

        Execution execution = executionApi.triggerAndWait(playbook.getId(), variables, 60);

        // Verify execution used variables
        assertThat(execution.isSuccessful()).isTrue();
        assertThat(execution.getInputVariables()).containsKey("target_url");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // EXECUTION MONITORING TESTS
    // ═══════════════════════════════════════════════════════════════════════════

    @Test(description = "Monitor execution progress in real-time")
    @Story("Execution Monitoring")
    @Severity(SeverityLevel.NORMAL)
    public void testMonitorExecutionProgress() {
        // Setup: Create multi-step playbook
        Playbook playbook = createMultiStepPlaybook("Progress Monitor Test");

        // Trigger execution
        Execution execution = executionApi.triggerExecution(playbook.getId());

        // Navigate to execution page
        executionPage.navigate(execution.getId());

        // Verify initial state
        assertThat(executionPage.isRunning() || executionPage.isSuccessful()).isTrue();

        // Wait and verify progress updates
        executionPage.waitForCompletion(120);

        // Verify all steps shown
        assertThat(executionPage.getStepIds()).hasSize(3);
    }

    @Test(description = "View execution logs")
    @Story("Execution Monitoring")
    @Severity(SeverityLevel.NORMAL)
    public void testViewExecutionLogs() {
        // Setup and execute
        Playbook playbook = createTestPlaybook("Logs Test");
        Execution execution = executionApi.triggerAndWait(playbook.getId(), 60);

        // View logs via UI
        executionPage.navigate(execution.getId());
        executionPage.openLogsTab();

        // Verify logs present
        assertThat(executionPage.getLogCount())
            .as("Execution should have logs")
            .isGreaterThan(0);

        // Verify logs via API
        var logs = executionApi.getLogs(execution.getId());
        assertThat(logs).isNotEmpty();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // EXECUTION CONTROL TESTS
    // ═══════════════════════════════════════════════════════════════════════════

    @Test(description = "Cancel running execution")
    @Story("Execution Control")
    @Severity(SeverityLevel.NORMAL)
    public void testCancelExecution() {
        // Create slow playbook
        Playbook playbook = createSlowPlaybook("Cancel Test");

        // Trigger execution
        Execution execution = executionApi.triggerExecution(playbook.getId());

        // Wait for it to start running
        executionApi.waitForStatus(execution.getId(), Execution.ExecutionStatus.RUNNING, 10);

        // Cancel via UI
        executionPage.navigate(execution.getId());
        executionPage.cancelExecution();

        // Verify cancelled
        Execution cancelled = executionApi.getExecution(execution.getId());
        assertThat(cancelled.getStatus()).isEqualTo(Execution.ExecutionStatus.CANCELLED);
    }

    @Test(description = "Retry failed execution")
    @Story("Execution Control")
    @Severity(SeverityLevel.NORMAL)
    public void testRetryFailedExecution() {
        // Create playbook that will fail initially
        Playbook playbook = createFailingPlaybook("Retry Test");

        // Execute and expect failure
        Execution firstExecution = executionApi.triggerAndWait(playbook.getId(), 60);
        assertThat(firstExecution.isFailed()).isTrue();

        // Fix the issue (in real scenario would fix config)
        // Then retry
        Execution retryExecution = executionApi.retryExecution(firstExecution.getId());

        // Verify retry is a new execution
        assertThat(retryExecution.getId()).isNotEqualTo(firstExecution.getId());
        assertThat(retryExecution.getAttemptNumber()).isGreaterThan(firstExecution.getAttemptNumber());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // INTEGRATION TESTS WITH MOCKS
    // ═══════════════════════════════════════════════════════════════════════════

    @Test(description = "Verify Slack notification is sent")
    @Story("Integrations")
    @Severity(SeverityLevel.CRITICAL)
    public void testSlackNotificationSent() throws Exception {
        // Create playbook with Slack step pointing to mock
        Playbook playbook = createSlackPlaybook("Slack Integration Test", mockSlack.getWebhookUrl());

        // Execute
        Execution execution = executionApi.triggerAndWait(playbook.getId(), 60);
        assertThat(execution.isSuccessful()).isTrue();

        // Verify Slack received message
        mockSlack.waitForMessage(5000);
        assertThat(mockSlack.getMessageCount()).isEqualTo(1);

        var message = mockSlack.getLastMessage();
        assertThat(message.getText()).contains("Integration Test");
    }

    @Test(description = "Handle Slack failure gracefully")
    @Story("Integrations")
    @Severity(SeverityLevel.NORMAL)
    public void testSlackFailureHandling() {
        // Configure mock to fail
        mockSlack.configureFailure(500, "Internal Server Error");

        // Create playbook
        Playbook playbook = createSlackPlaybook("Slack Failure Test", mockSlack.getWebhookUrl());

        // Execute - should handle failure based on error handling config
        Execution execution = executionApi.triggerAndWait(playbook.getId(), 60);

        // Verify step failed but execution completed (based on error handling)
        assertThat(execution.getFailedStepCount()).isGreaterThan(0);
        mockSlack.assertMessageContains("Slack Failure Test"); // Message was attempted
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // HELPER METHODS
    // ═══════════════════════════════════════════════════════════════════════════

    private Playbook createTestPlaybook(String name) {
        Playbook.PlaybookStep httpStep = Playbook.PlaybookStep.builder()
            .id("step1")
            .name("HTTP Call")
            .type(StepType.HTTP_REQUEST)
            .order(1)
            .config(Map.of(
                "url", "https://httpbin.org/get",
                "method", "GET"
            ))
            .build();

        Playbook playbook = Playbook.builder()
            .name(name)
            .description("Test playbook")
            .status(Playbook.PlaybookStatus.ACTIVE)
            .steps(List.of(httpStep))
            .build();

        return playbookApi.createPlaybook(playbook);
    }

    private Playbook createParameterizedPlaybook(String name) {
        Playbook playbook = Playbook.builder()
            .name(name)
            .status(Playbook.PlaybookStatus.ACTIVE)
            .variables(Map.of(
                "target_url", Playbook.Variable.builder()
                    .name("target_url")
                    .type(Playbook.VariableType.STRING)
                    .required(true)
                    .build(),
                "message", Playbook.Variable.builder()
                    .name("message")
                    .type(Playbook.VariableType.STRING)
                    .defaultValue("Default message")
                    .build()
            ))
            .steps(List.of(
                Playbook.PlaybookStep.builder()
                    .id("step1")
                    .name("Call URL")
                    .type(StepType.HTTP_REQUEST)
                    .order(1)
                    .config(Map.of("url", "{{target_url}}"))
                    .build()
            ))
            .build();

        return playbookApi.createPlaybook(playbook);
    }

    private Playbook createMultiStepPlaybook(String name) {
        return playbookApi.createPlaybook(Playbook.builder()
            .name(name)
            .status(Playbook.PlaybookStatus.ACTIVE)
            .steps(List.of(
                Playbook.PlaybookStep.builder()
                    .id("step1").name("Step 1").type(StepType.HTTP_REQUEST).order(1)
                    .config(Map.of("url", "https://httpbin.org/get")).build(),
                Playbook.PlaybookStep.builder()
                    .id("step2").name("Wait").type(StepType.WAIT).order(2)
                    .config(Map.of("seconds", 2)).build(),
                Playbook.PlaybookStep.builder()
                    .id("step3").name("Step 3").type(StepType.HTTP_REQUEST).order(3)
                    .config(Map.of("url", "https://httpbin.org/post")).build()
            ))
            .build());
    }

    private Playbook createSlowPlaybook(String name) {
        return playbookApi.createPlaybook(Playbook.builder()
            .name(name)
            .status(Playbook.PlaybookStatus.ACTIVE)
            .steps(List.of(
                Playbook.PlaybookStep.builder()
                    .id("step1").name("Long Wait").type(StepType.WAIT).order(1)
                    .config(Map.of("seconds", 60)).build()
            ))
            .build());
    }

    private Playbook createFailingPlaybook(String name) {
        return playbookApi.createPlaybook(Playbook.builder()
            .name(name)
            .status(Playbook.PlaybookStatus.ACTIVE)
            .steps(List.of(
                Playbook.PlaybookStep.builder()
                    .id("step1").name("Failing Step").type(StepType.HTTP_REQUEST).order(1)
                    .config(Map.of("url", "https://httpbin.org/status/500")).build()
            ))
            .build());
    }

    private Playbook createSlackPlaybook(String name, String webhookUrl) {
        return playbookApi.createPlaybook(Playbook.builder()
            .name(name)
            .status(Playbook.PlaybookStatus.ACTIVE)
            .steps(List.of(
                Playbook.PlaybookStep.builder()
                    .id("step1").name("Notify Slack").type(StepType.SEND_SLACK).order(1)
                    .config(Map.of(
                        "webhookUrl", webhookUrl,
                        "channel", "#test",
                        "message", name + " - Test message"
                    )).build()
            ))
            .build());
    }
}
