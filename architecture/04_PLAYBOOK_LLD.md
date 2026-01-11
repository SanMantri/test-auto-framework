# Playbook Domain - Low-Level Design (LLD)

## Document Information
| Attribute | Value |
|-----------|-------|
| Domain | Playbook (Workflow Automation) |
| Version | 1.0 |
| Dependencies | Master HLD |

---

## 1. Domain Overview

### 1.1 What is a Playbook?

A **Playbook** is a configurable, reusable sequence of automated actions. Think of it like:
- **Zapier/Make** workflows
- **GitHub Actions** workflows
- **n8n** automation sequences

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                           PLAYBOOK DOMAIN SCOPE                                          │
├─────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                          │
│  WHAT IS A PLAYBOOK?                                                                     │
│  ───────────────────                                                                     │
│                                                                                          │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐   │
│  │                                                                                  │   │
│  │  TRIGGER ──▶ ACTION 1 ──▶ ACTION 2 ──▶ CONDITION ──▶ ACTION 3 ──▶ OUTPUT       │   │
│  │                                            │                                     │   │
│  │                                            └──▶ ACTION 4 (else branch)          │   │
│  │                                                                                  │   │
│  └─────────────────────────────────────────────────────────────────────────────────┘   │
│                                                                                          │
│  EXAMPLE PLAYBOOKS:                                                                      │
│  ──────────────────                                                                      │
│                                                                                          │
│  1. "New Lead Notification"                                                              │
│     Trigger: Form submission                                                             │
│     Actions: Create CRM contact → Send Slack notification → Send welcome email          │
│                                                                                          │
│  2. "Daily Report Generator"                                                             │
│     Trigger: Schedule (9 AM daily)                                                       │
│     Actions: Query database → Generate PDF → Upload to S3 → Email to team              │
│                                                                                          │
│  3. "Order Fulfillment"                                                                  │
│     Trigger: Order.created webhook                                                       │
│     Actions: Validate inventory → Create shipment → Update status → Notify customer    │
│                                                                                          │
│  TESTING CHALLENGES:                                                                     │
│  ────────────────────                                                                    │
│  • Complex branching logic                                                              │
│  • External integrations (Slack, Email, CRM)                                            │
│  • Time-based triggers                                                                  │
│  • Stateful execution (multi-step with persistence)                                     │
│  • Error recovery and retries                                                           │
│                                                                                          │
└─────────────────────────────────────────────────────────────────────────────────────────┘
```

### 1.2 Testing Philosophy for Playbooks

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                        PLAYBOOK TESTING PHILOSOPHY                                       │
├─────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                          │
│  PRINCIPLE: "Build via UI, Trigger via API, Verify via API"                            │
│                                                                                          │
│  WHY?                                                                                    │
│  ────                                                                                    │
│  • Playbook BUILDER is complex UI with drag-drop - must test UI                         │
│  • Playbook EXECUTION should be tested via API (faster, reliable)                       │
│  • External integrations should be MOCKED (no real Slack/email in tests)               │
│                                                                                          │
│  TESTING LAYERS:                                                                         │
│  ────────────────                                                                        │
│                                                                                          │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐   │
│  │                                                                                  │   │
│  │  LAYER 1: BUILDER UI TESTS                                                       │   │
│  │  ─────────────────────────                                                       │   │
│  │  • Can add/remove actions                                                        │   │
│  │  • Can configure action parameters                                               │   │
│  │  • Can create conditions/branches                                                │   │
│  │  • Drag-drop works correctly                                                     │   │
│  │  • Validation errors shown                                                       │   │
│  │                                                                                  │   │
│  │  LAYER 2: EXECUTION ENGINE TESTS (API)                                           │   │
│  │  ────────────────────────────────────                                            │   │
│  │  • Playbook executes in correct order                                            │   │
│  │  • Conditions evaluate correctly                                                 │   │
│  │  • Data passed between steps                                                     │   │
│  │  • Errors handled gracefully                                                     │   │
│  │  • Retries work                                                                  │   │
│  │                                                                                  │   │
│  │  LAYER 3: INTEGRATION TESTS (Mocked)                                             │   │
│  │  ───────────────────────────────────                                             │   │
│  │  • Slack integration sends correct payload                                       │   │
│  │  • Email action uses correct template                                            │   │
│  │  • CRM creates record with correct fields                                        │   │
│  │  • Webhooks called with expected data                                            │   │
│  │                                                                                  │   │
│  └─────────────────────────────────────────────────────────────────────────────────┘   │
│                                                                                          │
└─────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Component Architecture

### 2.1 Playbook Module Structure

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                          PLAYBOOK MODULE COMPONENTS                                      │
├─────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                          │
│  playbook/                                                                               │
│  ├── api/                              # API Layer                                      │
│  │   ├── PlaybookApiClient.java        # CRUD for playbooks                             │
│  │   ├── ExecutionApiClient.java       # Trigger and monitor executions                 │
│  │   ├── ActionRegistryClient.java     # Available actions catalog                      │
│  │   └── TriggerApiClient.java         # Trigger configurations                         │
│  │                                                                                       │
│  ├── pages/                            # UI Layer (Page Objects)                        │
│  │   ├── PlaybookListPage.java         # Playbook catalog                               │
│  │   ├── PlaybookBuilderPage.java      # Visual workflow builder                        │
│  │   ├── PlaybookSettingsPage.java     # Playbook configuration                         │
│  │   ├── ExecutionHistoryPage.java     # Execution logs                                 │
│  │   └── components/                   # Reusable UI components                         │
│  │       ├── WorkflowCanvas.java       # Drag-drop canvas                               │
│  │       ├── ActionNode.java           # Action block in canvas                         │
│  │       ├── ConditionNode.java        # Condition/branch block                         │
│  │       ├── ActionConfigPanel.java    # Action configuration sidebar                   │
│  │       └── TriggerSelector.java      # Trigger type selection                         │
│  │                                                                                       │
│  ├── models/                           # Data Models                                    │
│  │   ├── Playbook.java                 # Playbook definition                            │
│  │   ├── Action.java                   # Action definition                              │
│  │   ├── Trigger.java                  # Trigger configuration                          │
│  │   ├── Condition.java                # Branch condition                               │
│  │   ├── Execution.java                # Execution instance                             │
│  │   ├── StepResult.java               # Individual step output                         │
│  │   └── builders/                                                                      │
│  │       ├── PlaybookBuilder.java      # Fluent playbook creation                       │
│  │       └── ActionBuilder.java        # Fluent action configuration                    │
│  │                                                                                       │
│  ├── playbooks/                        # Reusable Test Workflows                        │
│  │   ├── PlaybookCreationFlow.java     # Create playbook via UI                         │
│  │   ├── ExecutionVerificationFlow.java# Verify execution results                       │
│  │   └── IntegrationMockSetup.java     # Mock external services                         │
│  │                                                                                       │
│  ├── mocks/                            # Mock Services                                  │
│  │   ├── MockSlackServer.java          # Fake Slack API                                 │
│  │   ├── MockEmailServer.java          # Fake SMTP                                      │
│  │   ├── MockWebhookReceiver.java      # Captures webhook calls                         │
│  │   └── MockCRMClient.java            # Fake CRM responses                             │
│  │                                                                                       │
│  ├── tests/                            # Test Classes                                   │
│  │   ├── PlaybookBuilderTests.java     # UI builder tests                               │
│  │   ├── PlaybookExecutionTests.java   # Execution engine tests                         │
│  │   ├── ActionTests.java              # Individual action tests                        │
│  │   ├── ConditionTests.java           # Branching logic tests                          │
│  │   └── IntegrationTests.java         # External integration tests                     │
│  │                                                                                       │
│  └── data/                             # Test Data                                      │
│      ├── sample-playbooks.json         # Pre-built playbook templates                   │
│      ├── action-configs.json           # Action configuration samples                   │
│      └── execution-scenarios.json      # Test execution inputs                          │
│                                                                                          │
└─────────────────────────────────────────────────────────────────────────────────────────┘
```

### 2.2 Playbook Data Model

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                           PLAYBOOK DATA MODEL                                            │
├─────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                          │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐   │
│  │  {                                                                               │   │
│  │    "id": "PB-001",                                                               │   │
│  │    "name": "New Lead Notification",                                              │   │
│  │    "status": "ACTIVE",                                                           │   │
│  │    "version": 3,                                                                 │   │
│  │                                                                                  │   │
│  │    "trigger": {                                                                  │   │
│  │      "type": "WEBHOOK",                                                          │   │
│  │      "config": {                                                                 │   │
│  │        "path": "/hooks/new-lead",                                                │   │
│  │        "method": "POST",                                                         │   │
│  │        "authentication": "API_KEY"                                               │   │
│  │      }                                                                           │   │
│  │    },                                                                            │   │
│  │                                                                                  │   │
│  │    "steps": [                                                                    │   │
│  │      {                                                                           │   │
│  │        "id": "step-1",                                                           │   │
│  │        "type": "ACTION",                                                         │   │
│  │        "action": "crm.create_contact",                                           │   │
│  │        "config": {                                                               │   │
│  │          "name": "{{trigger.data.name}}",                                        │   │
│  │          "email": "{{trigger.data.email}}",                                      │   │
│  │          "source": "Website"                                                     │   │
│  │        },                                                                        │   │
│  │        "output": "contact"                                                       │   │
│  │      },                                                                          │   │
│  │      {                                                                           │   │
│  │        "id": "step-2",                                                           │   │
│  │        "type": "CONDITION",                                                      │   │
│  │        "condition": "{{trigger.data.company_size}} > 100",                       │   │
│  │        "then": "step-3",                                                         │   │
│  │        "else": "step-4"                                                          │   │
│  │      },                                                                          │   │
│  │      {                                                                           │   │
│  │        "id": "step-3",                                                           │   │
│  │        "type": "ACTION",                                                         │   │
│  │        "action": "slack.send_message",                                           │   │
│  │        "config": {                                                               │   │
│  │          "channel": "#enterprise-leads",                                         │   │
│  │          "message": "🎯 New enterprise lead: {{contact.name}}"                   │   │
│  │        }                                                                         │   │
│  │      },                                                                          │   │
│  │      {                                                                           │   │
│  │        "id": "step-4",                                                           │   │
│  │        "type": "ACTION",                                                         │   │
│  │        "action": "email.send",                                                   │   │
│  │        "config": {                                                               │   │
│  │          "to": "{{contact.email}}",                                              │   │
│  │          "template": "welcome-email"                                             │   │
│  │        }                                                                         │   │
│  │      }                                                                           │   │
│  │    ]                                                                             │   │
│  │  }                                                                               │   │
│  └─────────────────────────────────────────────────────────────────────────────────┘   │
│                                                                                          │
│  EXECUTION INSTANCE:                                                                     │
│  ───────────────────                                                                     │
│                                                                                          │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐   │
│  │  {                                                                               │   │
│  │    "executionId": "EXEC-2024-001",                                               │   │
│  │    "playbookId": "PB-001",                                                       │   │
│  │    "playbookVersion": 3,                                                         │   │
│  │    "status": "COMPLETED",                                                        │   │
│  │    "startedAt": "2024-01-15T10:30:00Z",                                          │   │
│  │    "completedAt": "2024-01-15T10:30:05Z",                                        │   │
│  │    "triggerData": { "name": "John", "email": "john@corp.com", ... },             │   │
│  │    "stepResults": [                                                              │   │
│  │      {                                                                           │   │
│  │        "stepId": "step-1",                                                       │   │
│  │        "status": "SUCCESS",                                                      │   │
│  │        "output": { "contact": { "id": "C-123", "name": "John" } },               │   │
│  │        "duration": 450                                                           │   │
│  │      },                                                                          │   │
│  │      {                                                                           │   │
│  │        "stepId": "step-2",                                                       │   │
│  │        "status": "SUCCESS",                                                      │   │
│  │        "conditionResult": true,                                                  │   │
│  │        "nextStep": "step-3"                                                      │   │
│  │      },                                                                          │   │
│  │      ...                                                                         │   │
│  │    ]                                                                             │   │
│  │  }                                                                               │   │
│  └─────────────────────────────────────────────────────────────────────────────────┘   │
│                                                                                          │
└─────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Page Objects Design

### 3.1 PlaybookBuilderPage Implementation

```java
/**
 * PlaybookBuilderPage - Visual workflow builder interface
 *
 * Handles drag-drop workflow creation, action configuration,
 * and playbook saving/publishing.
 */
public class PlaybookBuilderPage extends BasePage {

    // ═══════════════════════════════════════════════════════════════════════════
    // LOCATORS
    // ═══════════════════════════════════════════════════════════════════════════

    // Canvas
    private static final String WORKFLOW_CANVAS = "[data-testid='workflow-canvas']";
    private static final String ACTION_NODE = "[data-testid='action-node-%s']";
    private static final String CONDITION_NODE = "[data-testid='condition-node-%s']";
    private static final String CONNECTION_LINE = "[data-testid='connection-%s-%s']";

    // Action Palette
    private static final String ACTION_PALETTE = "[data-testid='action-palette']";
    private static final String ACTION_CATEGORY = "[data-testid='action-category-%s']";
    private static final String DRAGGABLE_ACTION = "[data-testid='draggable-action-%s']";

    // Configuration Panel
    private static final String CONFIG_PANEL = "[data-testid='config-panel']";
    private static final String CONFIG_INPUT = "[data-testid='config-%s']";
    private static final String VARIABLE_PICKER = "[data-testid='variable-picker']";

    // Toolbar
    private static final String SAVE_BUTTON = "[data-testid='save-playbook']";
    private static final String PUBLISH_BUTTON = "[data-testid='publish-playbook']";
    private static final String TEST_BUTTON = "[data-testid='test-playbook']";
    private static final String PLAYBOOK_NAME_INPUT = "[data-testid='playbook-name']";

    // ═══════════════════════════════════════════════════════════════════════════
    // NAVIGATION
    // ═══════════════════════════════════════════════════════════════════════════

    public PlaybookBuilderPage(Page page) {
        super(page);
    }

    public PlaybookBuilderPage navigateToNew() {
        page.navigate(baseUrl + "/playbooks/new");
        waitForCanvasReady();
        return this;
    }

    public PlaybookBuilderPage navigateToEdit(String playbookId) {
        page.navigate(baseUrl + "/playbooks/" + playbookId + "/edit");
        waitForCanvasReady();
        return this;
    }

    private void waitForCanvasReady() {
        page.waitForSelector(WORKFLOW_CANVAS);
        page.waitForSelector(ACTION_PALETTE);
        // Wait for drag-drop library to initialize
        page.waitForFunction("window.__workflowBuilderReady === true");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PLAYBOOK METADATA
    // ═══════════════════════════════════════════════════════════════════════════

    public PlaybookBuilderPage setPlaybookName(String name) {
        page.fill(PLAYBOOK_NAME_INPUT, name);
        return this;
    }

    public String getPlaybookName() {
        return page.inputValue(PLAYBOOK_NAME_INPUT);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TRIGGER CONFIGURATION
    // ═══════════════════════════════════════════════════════════════════════════

    public PlaybookBuilderPage selectTrigger(TriggerType type) {
        // Click on trigger node (usually at start of canvas)
        page.click("[data-testid='trigger-node']");

        // Select trigger type from dropdown
        page.selectOption("[data-testid='trigger-type']", type.getValue());

        return this;
    }

    public PlaybookBuilderPage configureTrigger(Map<String, String> config) {
        page.click("[data-testid='trigger-node']");

        for (Map.Entry<String, String> entry : config.entrySet()) {
            String locator = String.format(CONFIG_INPUT, entry.getKey());
            page.fill(locator, entry.getValue());
        }

        return this;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ACTION MANAGEMENT
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Adds an action to the workflow via drag-drop
     */
    public PlaybookBuilderPage addAction(ActionType actionType) {
        // Expand action category if needed
        String category = actionType.getCategory();
        Locator categoryLocator = page.locator(String.format(ACTION_CATEGORY, category));
        if (categoryLocator.getAttribute("aria-expanded").equals("false")) {
            categoryLocator.click();
        }

        // Get the draggable action
        String actionLocator = String.format(DRAGGABLE_ACTION, actionType.getValue());
        Locator action = page.locator(actionLocator);

        // Get the canvas
        Locator canvas = page.locator(WORKFLOW_CANVAS);

        // Perform drag-drop
        // Calculate drop position (end of current workflow)
        BoundingBox canvasBox = canvas.boundingBox();
        int dropX = (int) (canvasBox.x + canvasBox.width / 2);
        int dropY = (int) (canvasBox.y + canvasBox.height - 100);

        action.dragTo(canvas, new Locator.DragToOptions()
            .setTargetPosition(new Position(dropX - canvasBox.x, dropY - canvasBox.y)));

        // Wait for action to be added
        page.waitForSelector("[data-testid^='action-node-']:last-child");

        return this;
    }

    /**
     * Configures the currently selected action
     */
    public PlaybookBuilderPage configureAction(String actionId, Map<String, String> config) {
        // Click on action to select it
        page.click(String.format(ACTION_NODE, actionId));

        // Wait for config panel to show
        page.waitForSelector(CONFIG_PANEL);

        // Fill in configuration
        for (Map.Entry<String, String> entry : config.entrySet()) {
            String locator = String.format(CONFIG_INPUT, entry.getKey());

            if (entry.getValue().startsWith("{{")) {
                // Variable reference - use variable picker
                page.click(locator);
                page.click(VARIABLE_PICKER);
                selectVariable(entry.getValue());
            } else {
                // Static value
                page.fill(locator, entry.getValue());
            }
        }

        return this;
    }

    private void selectVariable(String variableRef) {
        // Variable format: {{step-1.output.fieldName}}
        String[] parts = variableRef.replace("{{", "").replace("}}", "").split("\\.");

        // Navigate variable tree
        for (String part : parts) {
            page.click(String.format("[data-testid='variable-%s']", part));
        }
    }

    /**
     * Connects two nodes in the workflow
     */
    public PlaybookBuilderPage connectNodes(String fromId, String toId) {
        // Get output port of source node
        Locator outputPort = page.locator(
            String.format("[data-testid='node-%s-output']", fromId));

        // Get input port of target node
        Locator inputPort = page.locator(
            String.format("[data-testid='node-%s-input']", toId));

        // Draw connection
        outputPort.dragTo(inputPort);

        // Verify connection created
        page.waitForSelector(String.format(CONNECTION_LINE, fromId, toId));

        return this;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CONDITION/BRANCHING
    // ═══════════════════════════════════════════════════════════════════════════

    public PlaybookBuilderPage addCondition() {
        // Add condition node (similar to addAction)
        page.click("[data-testid='add-condition-btn']");
        page.waitForSelector("[data-testid^='condition-node-']");
        return this;
    }

    public PlaybookBuilderPage configureCondition(String conditionId, String expression) {
        page.click(String.format(CONDITION_NODE, conditionId));
        page.fill("[data-testid='condition-expression']", expression);
        return this;
    }

    public PlaybookBuilderPage connectConditionBranch(
            String conditionId,
            BranchType branch,
            String targetNodeId) {

        String portId = branch == BranchType.THEN ? "then-port" : "else-port";
        Locator port = page.locator(
            String.format("[data-testid='condition-%s-%s']", conditionId, portId));

        Locator target = page.locator(
            String.format("[data-testid='node-%s-input']", targetNodeId));

        port.dragTo(target);
        return this;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SAVE & PUBLISH
    // ═══════════════════════════════════════════════════════════════════════════

    public PlaybookBuilderPage save() {
        page.click(SAVE_BUTTON);
        page.waitForSelector("[data-testid='save-success']");
        return this;
    }

    public PlaybookBuilderPage publish() {
        page.click(PUBLISH_BUTTON);
        page.waitForSelector("[data-testid='publish-confirm-modal']");
        page.click("[data-testid='confirm-publish']");
        page.waitForSelector("[data-testid='publish-success']");
        return this;
    }

    public String getPlaybookId() {
        // Extract from URL after save
        String url = page.url();
        return url.substring(url.lastIndexOf("/") + 1);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TESTING & VALIDATION
    // ═══════════════════════════════════════════════════════════════════════════

    public PlaybookBuilderPage testWithSampleData(Map<String, Object> testData) {
        page.click(TEST_BUTTON);

        // Fill test data modal
        page.waitForSelector("[data-testid='test-data-modal']");
        page.fill("[data-testid='test-data-input']", new Gson().toJson(testData));
        page.click("[data-testid='run-test']");

        // Wait for test execution
        page.waitForSelector("[data-testid='test-result']");

        return this;
    }

    public boolean isTestSuccessful() {
        return page.locator("[data-testid='test-result-success']").isVisible();
    }

    public List<String> getValidationErrors() {
        return page.locator("[data-testid='validation-error']").allTextContents();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // STATE QUERIES
    // ═══════════════════════════════════════════════════════════════════════════

    public int getActionCount() {
        return page.locator("[data-testid^='action-node-']").count();
    }

    public int getConditionCount() {
        return page.locator("[data-testid^='condition-node-']").count();
    }

    public List<String> getActionIds() {
        return page.locator("[data-testid^='action-node-']").all().stream()
            .map(l -> l.getAttribute("data-testid").replace("action-node-", ""))
            .collect(Collectors.toList());
    }
}
```

---

## 4. Playbook Test Patterns

### 4.1 Builder UI Tests

```java
/**
 * PlaybookBuilderTests - Tests for visual workflow builder
 */
@Test(groups = {"playbook", "ui", "builder"})
public class PlaybookBuilderTests extends BasePlaybookTest {

    // ═══════════════════════════════════════════════════════════════════════════
    // BASIC WORKFLOW CREATION
    // ═══════════════════════════════════════════════════════════════════════════

    @Test(description = "Create simple playbook with one action")
    public void createSimplePlaybook() {
        PlaybookBuilderPage builder = new PlaybookBuilderPage(page);
        builder.navigateToNew();

        // Set metadata
        builder.setPlaybookName("Test Playbook - Simple");

        // Configure trigger
        builder.selectTrigger(TriggerType.WEBHOOK);
        builder.configureTrigger(Map.of(
            "path", "/test-webhook",
            "method", "POST"
        ));

        // Add single action
        builder.addAction(ActionType.SLACK_SEND_MESSAGE);
        builder.configureAction("action-1", Map.of(
            "channel", "#test-channel",
            "message", "Hello from playbook!"
        ));

        // Save
        builder.save();

        // Verify
        String playbookId = builder.getPlaybookId();
        assertThat(playbookId).isNotNull();

        // Verify via API
        Playbook saved = playbookApi.get(playbookId);
        assertThat(saved.getName()).isEqualTo("Test Playbook - Simple");
        assertThat(saved.getSteps()).hasSize(1);
    }

    @Test(description = "Create playbook with conditional branching")
    public void createPlaybookWithCondition() {
        PlaybookBuilderPage builder = new PlaybookBuilderPage(page);
        builder.navigateToNew();

        builder.setPlaybookName("Test Playbook - Conditional");

        // Add trigger
        builder.selectTrigger(TriggerType.WEBHOOK);

        // Add condition
        builder.addCondition();
        builder.configureCondition("condition-1", "{{trigger.data.priority}} == 'HIGH'");

        // Add THEN branch action
        builder.addAction(ActionType.SLACK_SEND_MESSAGE);
        builder.configureAction("action-1", Map.of(
            "channel", "#urgent",
            "message", "Urgent: {{trigger.data.message}}"
        ));
        builder.connectConditionBranch("condition-1", BranchType.THEN, "action-1");

        // Add ELSE branch action
        builder.addAction(ActionType.EMAIL_SEND);
        builder.configureAction("action-2", Map.of(
            "to", "team@example.com",
            "subject", "New notification",
            "body", "{{trigger.data.message}}"
        ));
        builder.connectConditionBranch("condition-1", BranchType.ELSE, "action-2");

        builder.save();

        // Verify structure via API
        Playbook saved = playbookApi.get(builder.getPlaybookId());
        assertThat(saved.getSteps()).hasSize(3);  // condition + 2 actions

        Step condition = saved.getSteps().get(0);
        assertThat(condition.getType()).isEqualTo(StepType.CONDITION);
        assertThat(condition.getThenStep()).isEqualTo("action-1");
        assertThat(condition.getElseStep()).isEqualTo("action-2");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DRAG-DROP TESTS
    // ═══════════════════════════════════════════════════════════════════════════

    @Test(description = "Drag-drop action reordering works")
    public void reorderActionsViaDragDrop() {
        // Create playbook with 3 actions via API
        String playbookId = playbookApi.create(PlaybookBuilder.aPlaybook()
            .withAction("action-1", ActionType.HTTP_REQUEST)
            .withAction("action-2", ActionType.TRANSFORM_DATA)
            .withAction("action-3", ActionType.SLACK_SEND_MESSAGE)
            .build()
        ).getId();

        PlaybookBuilderPage builder = new PlaybookBuilderPage(page);
        builder.navigateToEdit(playbookId);

        // Verify initial order
        List<String> initialOrder = builder.getActionIds();
        assertThat(initialOrder).containsExactly("action-1", "action-2", "action-3");

        // Drag action-3 to position before action-1
        Locator action3 = page.locator("[data-testid='action-node-action-3']");
        Locator action1 = page.locator("[data-testid='action-node-action-1']");

        BoundingBox action1Box = action1.boundingBox();
        action3.dragTo(action1, new Locator.DragToOptions()
            .setTargetPosition(new Position(0, -10)));  // Drop above action-1

        // Verify new order
        List<String> newOrder = builder.getActionIds();
        assertThat(newOrder).containsExactly("action-3", "action-1", "action-2");

        // Save and verify persistence
        builder.save();
        Playbook updated = playbookApi.get(playbookId);
        assertThat(updated.getSteps().stream()
            .map(Step::getId)
            .collect(Collectors.toList()))
            .containsExactly("action-3", "action-1", "action-2");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // VARIABLE BINDING TESTS
    // ═══════════════════════════════════════════════════════════════════════════

    @Test(description = "Variable picker shows available variables")
    public void variablePickerShowsAvailableVariables() {
        PlaybookBuilderPage builder = new PlaybookBuilderPage(page);
        builder.navigateToNew();

        // Configure trigger with known schema
        builder.selectTrigger(TriggerType.WEBHOOK);

        // Add first action with output
        builder.addAction(ActionType.CRM_CREATE_CONTACT);
        builder.configureAction("action-1", Map.of(
            "name", "{{trigger.data.name}}",
            "email", "{{trigger.data.email}}"
        ));

        // Add second action
        builder.addAction(ActionType.SLACK_SEND_MESSAGE);

        // Click on message field and open variable picker
        page.click("[data-testid='config-message']");
        page.click(VARIABLE_PICKER);

        // Verify available variables
        assertThat(page.locator("[data-testid='variable-trigger']").isVisible()).isTrue();
        assertThat(page.locator("[data-testid='variable-action-1']").isVisible()).isTrue();

        // Expand action-1 to see its output
        page.click("[data-testid='variable-action-1']");
        assertThat(page.locator("[data-testid='variable-action-1.output']").isVisible()).isTrue();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // VALIDATION TESTS
    // ═══════════════════════════════════════════════════════════════════════════

    @Test(description = "Missing required configuration shows validation error")
    public void validationShowsRequiredFieldErrors() {
        PlaybookBuilderPage builder = new PlaybookBuilderPage(page);
        builder.navigateToNew();

        builder.setPlaybookName("Test - Missing Config");
        builder.selectTrigger(TriggerType.WEBHOOK);

        // Add action without configuring required fields
        builder.addAction(ActionType.SLACK_SEND_MESSAGE);
        // Don't configure channel and message

        // Try to save
        page.click(SAVE_BUTTON);

        // Verify validation errors
        List<String> errors = builder.getValidationErrors();
        assertThat(errors).isNotEmpty();
        assertThat(errors).anyMatch(e -> e.contains("channel"));
        assertThat(errors).anyMatch(e -> e.contains("message"));
    }
}
```

### 4.2 Execution Engine Tests

```java
/**
 * PlaybookExecutionTests - Tests for playbook execution engine
 *
 * These are mostly API tests - we trigger playbooks and verify execution
 */
@Test(groups = {"playbook", "api", "execution"})
public class PlaybookExecutionTests extends BasePlaybookTest {

    private MockSlackServer mockSlack;
    private MockEmailServer mockEmail;

    @BeforeClass
    public void setupMocks() {
        // Start mock servers
        mockSlack = new MockSlackServer(8081);
        mockEmail = new MockEmailServer(8082);

        mockSlack.start();
        mockEmail.start();

        // Configure playbook engine to use mocks
        configApi.setIntegrationEndpoint("slack", "http://localhost:8081");
        configApi.setIntegrationEndpoint("email", "http://localhost:8082");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // LINEAR EXECUTION TESTS
    // ═══════════════════════════════════════════════════════════════════════════

    @Test(description = "Playbook executes actions in order")
    public void linearExecutionOrder() {
        // Create playbook with 3 actions
        String playbookId = playbookApi.create(PlaybookBuilder.aPlaybook()
            .withName("Linear Test")
            .withTrigger(TriggerType.WEBHOOK, "/test-linear")
            .withAction("step-1", ActionType.HTTP_REQUEST)
                .config("url", "http://api.test/step1")
            .withAction("step-2", ActionType.TRANSFORM_DATA)
                .config("transform", "{{step-1.response}}.toUpperCase()")
            .withAction("step-3", ActionType.SLACK_SEND_MESSAGE)
                .config("channel", "#test")
                .config("message", "{{step-2.output}}")
            .build()
        ).getId();

        // Activate playbook
        playbookApi.activate(playbookId);

        // Trigger execution
        ExecutionResult result = executionApi.trigger(playbookId, Map.of(
            "input", "hello world"
        ));

        // Wait for completion
        Execution execution = executionApi.waitForCompletion(result.getExecutionId(), 30);

        // Verify
        assertThat(execution.getStatus()).isEqualTo(ExecutionStatus.COMPLETED);
        assertThat(execution.getStepResults()).hasSize(3);

        // Verify order
        List<String> executedSteps = execution.getStepResults().stream()
            .map(StepResult::getStepId)
            .collect(Collectors.toList());
        assertThat(executedSteps).containsExactly("step-1", "step-2", "step-3");

        // Verify Slack received message
        assertThat(mockSlack.getReceivedMessages()).hasSize(1);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CONDITIONAL EXECUTION TESTS
    // ═══════════════════════════════════════════════════════════════════════════

    @Test(description = "Condition evaluates to true - THEN branch executes")
    public void conditionTrueBranch() {
        String playbookId = createConditionalPlaybook();
        playbookApi.activate(playbookId);

        // Trigger with data that makes condition TRUE
        ExecutionResult result = executionApi.trigger(playbookId, Map.of(
            "priority", "HIGH",
            "message", "Urgent issue"
        ));

        Execution execution = executionApi.waitForCompletion(result.getExecutionId(), 30);

        // Verify THEN branch executed (Slack)
        assertThat(execution.getStepResults()).anyMatch(
            s -> s.getStepId().equals("slack-action") && s.getStatus() == StepStatus.SUCCESS
        );

        // Verify ELSE branch did NOT execute (Email)
        assertThat(execution.getStepResults()).noneMatch(
            s -> s.getStepId().equals("email-action")
        );

        // Verify Slack received message
        assertThat(mockSlack.getReceivedMessages()).hasSize(1);
        assertThat(mockSlack.getLastMessage().getChannel()).isEqualTo("#urgent");
    }

    @Test(description = "Condition evaluates to false - ELSE branch executes")
    public void conditionFalseBranch() {
        String playbookId = createConditionalPlaybook();
        playbookApi.activate(playbookId);

        // Trigger with data that makes condition FALSE
        ExecutionResult result = executionApi.trigger(playbookId, Map.of(
            "priority", "LOW",
            "message": "Regular notification"
        ));

        Execution execution = executionApi.waitForCompletion(result.getExecutionId(), 30);

        // Verify ELSE branch executed (Email)
        assertThat(execution.getStepResults()).anyMatch(
            s -> s.getStepId().equals("email-action") && s.getStatus() == StepStatus.SUCCESS
        );

        // Verify THEN branch did NOT execute (Slack)
        assertThat(execution.getStepResults()).noneMatch(
            s -> s.getStepId().equals("slack-action")
        );

        // Verify Email received
        assertThat(mockEmail.getReceivedEmails()).hasSize(1);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ERROR HANDLING TESTS
    // ═══════════════════════════════════════════════════════════════════════════

    @Test(description = "Action failure marks execution as failed")
    public void actionFailureHandling() {
        // Create playbook with action that will fail
        String playbookId = playbookApi.create(PlaybookBuilder.aPlaybook()
            .withAction("failing-action", ActionType.HTTP_REQUEST)
                .config("url", "http://localhost:9999/nonexistent")  // Will fail
            .build()
        ).getId();

        playbookApi.activate(playbookId);

        ExecutionResult result = executionApi.trigger(playbookId, Map.of());
        Execution execution = executionApi.waitForCompletion(result.getExecutionId(), 30);

        // Verify failure
        assertThat(execution.getStatus()).isEqualTo(ExecutionStatus.FAILED);
        assertThat(execution.getStepResults().get(0).getStatus()).isEqualTo(StepStatus.FAILED);
        assertThat(execution.getStepResults().get(0).getError()).isNotNull();
    }

    @Test(description = "Retry mechanism works for transient failures")
    public void retryMechanismWorks() {
        // Configure mock to fail first 2 times, then succeed
        mockSlack.failNextRequests(2);

        String playbookId = playbookApi.create(PlaybookBuilder.aPlaybook()
            .withAction("slack-action", ActionType.SLACK_SEND_MESSAGE)
                .config("channel", "#test")
                .config("message", "Test")
                .retries(3)
            .build()
        ).getId();

        playbookApi.activate(playbookId);

        ExecutionResult result = executionApi.trigger(playbookId, Map.of());
        Execution execution = executionApi.waitForCompletion(result.getExecutionId(), 60);

        // Should succeed after retries
        assertThat(execution.getStatus()).isEqualTo(ExecutionStatus.COMPLETED);

        // Verify 3 attempts (2 failures + 1 success)
        StepResult slackStep = execution.getStepResult("slack-action");
        assertThat(slackStep.getAttempts()).isEqualTo(3);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════════════════

    private String createConditionalPlaybook() {
        return playbookApi.create(PlaybookBuilder.aPlaybook()
            .withName("Conditional Test")
            .withTrigger(TriggerType.WEBHOOK, "/test-conditional")
            .withCondition("check-priority", "{{trigger.data.priority}} == 'HIGH'")
                .thenStep("slack-action")
                .elseStep("email-action")
            .withAction("slack-action", ActionType.SLACK_SEND_MESSAGE)
                .config("channel", "#urgent")
                .config("message", "{{trigger.data.message}}")
            .withAction("email-action", ActionType.EMAIL_SEND)
                .config("to", "team@example.com")
                .config("subject", "Notification")
                .config("body", "{{trigger.data.message}}")
            .build()
        ).getId();
    }

    @AfterClass
    public void teardownMocks() {
        mockSlack.stop();
        mockEmail.stop();
    }
}
```

---

## 5. Mock Services Design

### 5.1 MockSlackServer

```java
/**
 * MockSlackServer - Simulates Slack API for testing
 */
public class MockSlackServer {

    private final int port;
    private HttpServer server;
    private final List<SlackMessage> receivedMessages = new ArrayList<>();
    private int failNextCount = 0;

    public MockSlackServer(int port) {
        this.port = port;
    }

    public void start() {
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);

            // POST /api/chat.postMessage
            server.createContext("/api/chat.postMessage", exchange -> {
                if (failNextCount > 0) {
                    failNextCount--;
                    exchange.sendResponseHeaders(500, 0);
                    exchange.close();
                    return;
                }

                // Parse request body
                String body = new String(exchange.getRequestBody().readAllBytes());
                SlackMessage message = parseSlackMessage(body);
                receivedMessages.add(message);

                // Send success response
                String response = "{\"ok\": true, \"ts\": \"" + System.currentTimeMillis() + "\"}";
                exchange.sendResponseHeaders(200, response.length());
                exchange.getResponseBody().write(response.getBytes());
                exchange.close();
            });

            server.setExecutor(Executors.newFixedThreadPool(4));
            server.start();

        } catch (IOException e) {
            throw new RuntimeException("Failed to start mock Slack server", e);
        }
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    public void failNextRequests(int count) {
        this.failNextCount = count;
    }

    public List<SlackMessage> getReceivedMessages() {
        return new ArrayList<>(receivedMessages);
    }

    public SlackMessage getLastMessage() {
        return receivedMessages.isEmpty() ? null : receivedMessages.get(receivedMessages.size() - 1);
    }

    public void clearMessages() {
        receivedMessages.clear();
    }

    private SlackMessage parseSlackMessage(String body) {
        // Parse URL-encoded or JSON body
        Map<String, String> params = parseParams(body);
        return new SlackMessage(
            params.get("channel"),
            params.get("text")
        );
    }
}

/**
 * Captured Slack message for assertions
 */
@Data
@AllArgsConstructor
public class SlackMessage {
    private String channel;
    private String text;
}
```

---

## 6. Key Test Scenarios Summary

| Category | Test | Type | Priority |
|----------|------|------|----------|
| **Builder** | Create simple playbook | UI | P1 |
| **Builder** | Add conditional branching | UI | P1 |
| **Builder** | Drag-drop reordering | UI | P2 |
| **Builder** | Variable picker | UI | P2 |
| **Builder** | Validation errors | UI | P1 |
| **Execution** | Linear execution order | API | P0 |
| **Execution** | Condition TRUE branch | API | P0 |
| **Execution** | Condition FALSE branch | API | P0 |
| **Execution** | Error handling | API | P1 |
| **Execution** | Retry mechanism | API | P1 |
| **Integration** | Slack notification sent | API + Mock | P1 |
| **Integration** | Email delivery | API + Mock | P1 |

---

*Document End - Playbook LLD v1.0*
