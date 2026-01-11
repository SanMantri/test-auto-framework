package com.framework.domains.playbook.pages;

import com.framework.core.base.BasePage;
import com.framework.domains.playbook.models.Playbook;
import com.framework.domains.playbook.models.Playbook.StepType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitForSelectorState;
import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * PlaybookBuilderPage - Visual playbook builder page object
 *
 * Handles interactions with the drag-and-drop playbook builder UI:
 * - Adding/removing steps
 * - Configuring step properties
 * - Setting triggers and variables
 * - Saving and activating playbooks
 */
@Slf4j
public class PlaybookBuilderPage extends BasePage {

    // ═══════════════════════════════════════════════════════════════════════════
    // LOCATORS
    // ═══════════════════════════════════════════════════════════════════════════

    // Canvas and Steps
    private static final String BUILDER_CANVAS = "[data-testid='playbook-canvas'], .playbook-canvas";
    private static final String STEP_PALETTE = "[data-testid='step-palette'], .step-palette";
    private static final String STEP_NODE = "[data-testid='step-node-%s']";
    private static final String STEP_NODES = "[data-testid^='step-node-']";
    private static final String ADD_STEP_BUTTON = "[data-testid='add-step-btn'], button:has-text('Add Step')";
    private static final String DELETE_STEP_BUTTON = "[data-testid='delete-step-%s']";

    // Step Types in Palette
    private static final String STEP_TYPE_ITEM = "[data-testid='step-type-%s']";

    // Step Configuration
    private static final String STEP_CONFIG_PANEL = "[data-testid='step-config-panel'], .step-config";
    private static final String STEP_NAME_INPUT = "[data-testid='step-name'], #step-name";
    private static final String STEP_CONFIG_FIELD = "[data-testid='config-%s'], input[name='%s']";
    private static final String STEP_SAVE_BUTTON = "[data-testid='save-step'], button:has-text('Save')";
    private static final String STEP_CANCEL_BUTTON = "[data-testid='cancel-step'], button:has-text('Cancel')";

    // HTTP Request Step
    private static final String HTTP_URL_INPUT = "[data-testid='http-url'], #http-url";
    private static final String HTTP_METHOD_SELECT = "[data-testid='http-method'], #http-method";
    private static final String HTTP_HEADERS_EDITOR = "[data-testid='http-headers']";
    private static final String HTTP_BODY_EDITOR = "[data-testid='http-body']";

    // Slack Step
    private static final String SLACK_CHANNEL_INPUT = "[data-testid='slack-channel'], #slack-channel";
    private static final String SLACK_MESSAGE_INPUT = "[data-testid='slack-message'], #slack-message";
    private static final String SLACK_WEBHOOK_INPUT = "[data-testid='slack-webhook'], #slack-webhook";

    // Email Step
    private static final String EMAIL_TO_INPUT = "[data-testid='email-to'], #email-to";
    private static final String EMAIL_SUBJECT_INPUT = "[data-testid='email-subject'], #email-subject";
    private static final String EMAIL_BODY_INPUT = "[data-testid='email-body'], #email-body";

    // Wait Step
    private static final String WAIT_DURATION_INPUT = "[data-testid='wait-duration'], #wait-duration";
    private static final String WAIT_UNIT_SELECT = "[data-testid='wait-unit'], #wait-unit";

    // Conditional Step
    private static final String CONDITION_EXPRESSION_INPUT = "[data-testid='condition-expr'], #condition-expression";
    private static final String CONDITION_TRUE_SELECT = "[data-testid='on-true'], #on-true";
    private static final String CONDITION_FALSE_SELECT = "[data-testid='on-false'], #on-false";

    // Variables
    private static final String VARIABLES_TAB = "[data-testid='variables-tab'], button:has-text('Variables')";
    private static final String ADD_VARIABLE_BUTTON = "[data-testid='add-variable'], button:has-text('Add Variable')";
    private static final String VARIABLE_NAME_INPUT = "[data-testid='var-name'], #var-name";
    private static final String VARIABLE_TYPE_SELECT = "[data-testid='var-type'], #var-type";
    private static final String VARIABLE_DEFAULT_INPUT = "[data-testid='var-default'], #var-default";
    private static final String VARIABLE_REQUIRED_CHECKBOX = "[data-testid='var-required'], #var-required";
    private static final String VARIABLE_ROW = "[data-testid='variable-%s']";

    // Trigger Configuration
    private static final String TRIGGER_TAB = "[data-testid='trigger-tab'], button:has-text('Trigger')";
    private static final String TRIGGER_TYPE_SELECT = "[data-testid='trigger-type'], #trigger-type";
    private static final String CRON_EXPRESSION_INPUT = "[data-testid='cron-expr'], #cron-expression";
    private static final String WEBHOOK_URL_DISPLAY = "[data-testid='webhook-url']";

    // Playbook Metadata
    private static final String PLAYBOOK_NAME_INPUT = "[data-testid='playbook-name'], #playbook-name";
    private static final String PLAYBOOK_DESCRIPTION = "[data-testid='playbook-desc'], #playbook-description";
    private static final String PLAYBOOK_TAGS_INPUT = "[data-testid='playbook-tags'], .tags-input";

    // Actions
    private static final String SAVE_PLAYBOOK_BUTTON = "[data-testid='save-playbook'], button:has-text('Save')";
    private static final String ACTIVATE_BUTTON = "[data-testid='activate-playbook'], button:has-text('Activate')";
    private static final String TEST_RUN_BUTTON = "[data-testid='test-run'], button:has-text('Test Run')";
    private static final String VALIDATE_BUTTON = "[data-testid='validate'], button:has-text('Validate')";

    // Status
    private static final String VALIDATION_ERRORS = "[data-testid='validation-errors']";
    private static final String SAVE_SUCCESS_TOAST = "[data-testid='save-success'], .toast-success";
    private static final String PLAYBOOK_STATUS = "[data-testid='playbook-status']";

    // ═══════════════════════════════════════════════════════════════════════════
    // CONSTRUCTOR & NAVIGATION
    // ═══════════════════════════════════════════════════════════════════════════

    public PlaybookBuilderPage(Page page) {
        super(page);
    }

    @Step("Navigate to playbook builder")
    public PlaybookBuilderPage navigate() {
        navigateTo("/playbooks/new");
        return this;
    }

    @Step("Navigate to edit playbook: {playbookId}")
    public PlaybookBuilderPage navigateToEdit(String playbookId) {
        navigateTo("/playbooks/" + playbookId + "/edit");
        return this;
    }

    @Override
    protected void waitForPageLoad() {
        waitForVisible(BUILDER_CANVAS);
    }

    @Override
    public boolean isDisplayed() {
        return isVisible(BUILDER_CANVAS);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PLAYBOOK METADATA
    // ═══════════════════════════════════════════════════════════════════════════

    @Step("Set playbook name: {name}")
    public PlaybookBuilderPage setPlaybookName(String name) {
        log.info("Setting playbook name: {}", name);
        fill(PLAYBOOK_NAME_INPUT, name);
        return this;
    }

    @Step("Set playbook description")
    public PlaybookBuilderPage setDescription(String description) {
        log.info("Setting playbook description");
        fill(PLAYBOOK_DESCRIPTION, description);
        return this;
    }

    @Step("Add tag: {tag}")
    public PlaybookBuilderPage addTag(String tag) {
        log.info("Adding tag: {}", tag);
        fill(PLAYBOOK_TAGS_INPUT, tag);
        page.keyboard().press("Enter");
        return this;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // STEP MANAGEMENT
    // ═══════════════════════════════════════════════════════════════════════════

    @Step("Add step of type: {stepType}")
    public PlaybookBuilderPage addStep(StepType stepType) {
        log.info("Adding step of type: {}", stepType);
        click(ADD_STEP_BUTTON);

        // Click on the step type in the palette
        String stepTypeLocator = String.format(STEP_TYPE_ITEM, stepType.name().toLowerCase());
        click(stepTypeLocator);

        // Wait for config panel to open
        waitForVisible(STEP_CONFIG_PANEL);
        return this;
    }

    @Step("Configure step name: {name}")
    public PlaybookBuilderPage setStepName(String name) {
        log.info("Setting step name: {}", name);
        fill(STEP_NAME_INPUT, name);
        return this;
    }

    @Step("Select step: {stepId}")
    public PlaybookBuilderPage selectStep(String stepId) {
        log.info("Selecting step: {}", stepId);
        String locator = String.format(STEP_NODE, stepId);
        click(locator);
        waitForVisible(STEP_CONFIG_PANEL);
        return this;
    }

    @Step("Delete step: {stepId}")
    public PlaybookBuilderPage deleteStep(String stepId) {
        log.info("Deleting step: {}", stepId);
        selectStep(stepId);
        String deleteLocator = String.format(DELETE_STEP_BUTTON, stepId);
        click(deleteLocator);
        return this;
    }

    @Step("Save current step configuration")
    public PlaybookBuilderPage saveStep() {
        log.info("Saving step configuration");
        click(STEP_SAVE_BUTTON);
        waitForHidden(STEP_CONFIG_PANEL, SHORT_TIMEOUT);
        return this;
    }

    @Step("Cancel step configuration")
    public PlaybookBuilderPage cancelStep() {
        click(STEP_CANCEL_BUTTON);
        return this;
    }

    public int getStepCount() {
        return page.locator(STEP_NODES).count();
    }

    public List<String> getStepIds() {
        return page.locator(STEP_NODES).all().stream()
            .map(l -> l.getAttribute("data-testid").replace("step-node-", ""))
            .toList();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // HTTP REQUEST STEP
    // ═══════════════════════════════════════════════════════════════════════════

    @Step("Configure HTTP request step")
    public PlaybookBuilderPage configureHttpStep(String url, String method, Map<String, String> headers, String body) {
        log.info("Configuring HTTP step: {} {}", method, url);

        fill(HTTP_URL_INPUT, url);
        selectOption(HTTP_METHOD_SELECT, method);

        if (headers != null && !headers.isEmpty()) {
            // Add headers in JSON format
            String headersJson = toJson(headers);
            fill(HTTP_HEADERS_EDITOR, headersJson);
        }

        if (body != null) {
            fill(HTTP_BODY_EDITOR, body);
        }

        return this;
    }

    @Step("Configure simple HTTP GET")
    public PlaybookBuilderPage configureHttpGet(String url) {
        return configureHttpStep(url, "GET", null, null);
    }

    @Step("Configure HTTP POST with JSON body")
    public PlaybookBuilderPage configureHttpPost(String url, String jsonBody) {
        return configureHttpStep(url, "POST", Map.of("Content-Type", "application/json"), jsonBody);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SLACK STEP
    // ═══════════════════════════════════════════════════════════════════════════

    @Step("Configure Slack notification step")
    public PlaybookBuilderPage configureSlackStep(String channel, String message) {
        log.info("Configuring Slack step: channel={}", channel);
        fill(SLACK_CHANNEL_INPUT, channel);
        fill(SLACK_MESSAGE_INPUT, message);
        return this;
    }

    @Step("Configure Slack webhook")
    public PlaybookBuilderPage setSlackWebhook(String webhookUrl) {
        log.info("Setting Slack webhook URL");
        fill(SLACK_WEBHOOK_INPUT, webhookUrl);
        return this;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // EMAIL STEP
    // ═══════════════════════════════════════════════════════════════════════════

    @Step("Configure Email step")
    public PlaybookBuilderPage configureEmailStep(String to, String subject, String body) {
        log.info("Configuring Email step: to={}", to);
        fill(EMAIL_TO_INPUT, to);
        fill(EMAIL_SUBJECT_INPUT, subject);
        fill(EMAIL_BODY_INPUT, body);
        return this;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // WAIT STEP
    // ═══════════════════════════════════════════════════════════════════════════

    @Step("Configure Wait step: {duration} {unit}")
    public PlaybookBuilderPage configureWaitStep(int duration, String unit) {
        log.info("Configuring Wait step: {} {}", duration, unit);
        fill(WAIT_DURATION_INPUT, String.valueOf(duration));
        selectOption(WAIT_UNIT_SELECT, unit);
        return this;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CONDITIONAL STEP
    // ═══════════════════════════════════════════════════════════════════════════

    @Step("Configure Conditional step")
    public PlaybookBuilderPage configureConditionalStep(String expression, String onTrueStepId, String onFalseStepId) {
        log.info("Configuring Conditional step: {}", expression);
        fill(CONDITION_EXPRESSION_INPUT, expression);
        if (onTrueStepId != null) {
            selectOption(CONDITION_TRUE_SELECT, onTrueStepId);
        }
        if (onFalseStepId != null) {
            selectOption(CONDITION_FALSE_SELECT, onFalseStepId);
        }
        return this;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // VARIABLES
    // ═══════════════════════════════════════════════════════════════════════════

    @Step("Open variables tab")
    public PlaybookBuilderPage openVariablesTab() {
        click(VARIABLES_TAB);
        return this;
    }

    @Step("Add variable: {name}")
    public PlaybookBuilderPage addVariable(String name, String type, String defaultValue, boolean required) {
        log.info("Adding variable: {} ({})", name, type);

        click(ADD_VARIABLE_BUTTON);
        fill(VARIABLE_NAME_INPUT, name);
        selectOption(VARIABLE_TYPE_SELECT, type);

        if (defaultValue != null) {
            fill(VARIABLE_DEFAULT_INPUT, defaultValue);
        }

        if (required) {
            click(VARIABLE_REQUIRED_CHECKBOX);
        }

        click(STEP_SAVE_BUTTON);
        return this;
    }

    @Step("Delete variable: {name}")
    public PlaybookBuilderPage deleteVariable(String name) {
        log.info("Deleting variable: {}", name);
        String variableLocator = String.format(VARIABLE_ROW, name);
        page.locator(variableLocator).locator("button:has-text('Delete')").click();
        return this;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TRIGGER CONFIGURATION
    // ═══════════════════════════════════════════════════════════════════════════

    @Step("Open trigger tab")
    public PlaybookBuilderPage openTriggerTab() {
        click(TRIGGER_TAB);
        return this;
    }

    @Step("Set trigger type: {triggerType}")
    public PlaybookBuilderPage setTriggerType(Playbook.TriggerType triggerType) {
        log.info("Setting trigger type: {}", triggerType);
        selectOption(TRIGGER_TYPE_SELECT, triggerType.name());
        return this;
    }

    @Step("Set cron schedule: {cronExpression}")
    public PlaybookBuilderPage setCronSchedule(String cronExpression) {
        log.info("Setting cron schedule: {}", cronExpression);
        fill(CRON_EXPRESSION_INPUT, cronExpression);
        return this;
    }

    public String getWebhookUrl() {
        return getText(WEBHOOK_URL_DISPLAY);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ACTIONS
    // ═══════════════════════════════════════════════════════════════════════════

    @Step("Save playbook")
    public PlaybookBuilderPage savePlaybook() {
        log.info("Saving playbook");
        click(SAVE_PLAYBOOK_BUTTON);
        waitForVisible(SAVE_SUCCESS_TOAST, 5000);
        return this;
    }

    @Step("Activate playbook")
    public PlaybookBuilderPage activatePlaybook() {
        log.info("Activating playbook");
        click(ACTIVATE_BUTTON);
        // Wait for status to change
        page.locator(PLAYBOOK_STATUS).waitFor(new Locator.WaitForOptions()
            .setState(WaitForSelectorState.VISIBLE));
        return this;
    }

    @Step("Validate playbook")
    public PlaybookBuilderPage validatePlaybook() {
        log.info("Validating playbook");
        click(VALIDATE_BUTTON);
        waitFor(1000); // Wait for validation to complete
        return this;
    }

    @Step("Trigger test run")
    public ExecutionPage triggerTestRun() {
        log.info("Triggering test run");
        click(TEST_RUN_BUTTON);
        return new ExecutionPage(page);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // STATUS & VALIDATION
    // ═══════════════════════════════════════════════════════════════════════════

    public boolean hasValidationErrors() {
        return isVisible(VALIDATION_ERRORS);
    }

    public String getValidationErrors() {
        if (hasValidationErrors()) {
            return getText(VALIDATION_ERRORS);
        }
        return null;
    }

    public String getPlaybookStatus() {
        return getText(PLAYBOOK_STATUS);
    }

    public boolean isSaved() {
        return isVisible(SAVE_SUCCESS_TOAST);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // BUILDER PATTERN FOR COMPLETE PLAYBOOK
    // ═══════════════════════════════════════════════════════════════════════════

    @Step("Create simple HTTP notification playbook")
    public PlaybookBuilderPage createHttpNotificationPlaybook(String name, String httpUrl, String slackChannel, String message) {
        log.info("Creating HTTP notification playbook: {}", name);

        setPlaybookName(name);
        setDescription("Automated HTTP notification playbook");

        // Add HTTP step
        addStep(StepType.HTTP_REQUEST);
        setStepName("Call API");
        configureHttpGet(httpUrl);
        saveStep();

        // Add Slack notification
        addStep(StepType.SEND_SLACK);
        setStepName("Notify Slack");
        configureSlackStep(slackChannel, message);
        saveStep();

        return this;
    }

    // Helper method to convert to JSON
    private String toJson(Map<String, String> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (!first) sb.append(",");
            sb.append("\"").append(entry.getKey()).append("\":\"").append(entry.getValue()).append("\"");
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }
}
