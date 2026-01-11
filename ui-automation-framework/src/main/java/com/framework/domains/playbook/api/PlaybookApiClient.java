package com.framework.domains.playbook.api;

import com.framework.core.base.BaseApiClient;
import com.framework.domains.playbook.models.Playbook;
import com.framework.domains.playbook.models.Playbook.PlaybookStatus;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * PlaybookApiClient - API operations for playbook management
 *
 * Handles CRUD operations for playbooks via API.
 * Used for test setup and verification of UI operations.
 */
@Slf4j
@Component
public class PlaybookApiClient extends BaseApiClient {

    @Override
    protected String getBasePath() {
        return "/api/v1/playbooks";
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PLAYBOOK CRUD
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Creates a new playbook.
     */
    public Playbook createPlaybook(Playbook playbook) {
        log.info("Creating playbook: {}", playbook.getName());
        Response response = post("", playbook);
        return getCreatedAs(response, Playbook.class);
    }

    /**
     * Creates a playbook from template.
     */
    public Playbook createFromTemplate(String templateId, String name) {
        log.info("Creating playbook from template: {} with name: {}", templateId, name);
        Response response = post("/from-template", Map.of(
            "templateId", templateId,
            "name", name
        ));
        return getCreatedAs(response, Playbook.class);
    }

    /**
     * Gets a playbook by ID.
     */
    public Playbook getPlaybook(String playbookId) {
        log.info("Getting playbook: {}", playbookId);
        Response response = get("/" + playbookId);
        return getOkAs(response, Playbook.class);
    }

    /**
     * Gets all playbooks for the current user.
     */
    public List<Playbook> getMyPlaybooks() {
        log.info("Getting my playbooks");
        Response response = get("");
        return List.of(getOkAs(response, Playbook[].class));
    }

    /**
     * Gets playbooks with filters.
     */
    public List<Playbook> getPlaybooks(Map<String, Object> filters) {
        log.info("Getting playbooks with filters: {}", filters);
        Response response = get("", filters);
        return List.of(getOkAs(response, Playbook[].class));
    }

    /**
     * Updates a playbook.
     */
    public Playbook updatePlaybook(String playbookId, Playbook playbook) {
        log.info("Updating playbook: {}", playbookId);
        Response response = put("/" + playbookId, playbook);
        return getOkAs(response, Playbook.class);
    }

    /**
     * Deletes a playbook.
     */
    public void deletePlaybook(String playbookId) {
        log.info("Deleting playbook: {}", playbookId);
        Response response = delete("/" + playbookId);
        assertNoContent(response);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PLAYBOOK STATUS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Activates a playbook.
     */
    public Playbook activatePlaybook(String playbookId) {
        log.info("Activating playbook: {}", playbookId);
        Response response = post("/" + playbookId + "/activate");
        return getOkAs(response, Playbook.class);
    }

    /**
     * Pauses a playbook.
     */
    public Playbook pausePlaybook(String playbookId) {
        log.info("Pausing playbook: {}", playbookId);
        Response response = post("/" + playbookId + "/pause");
        return getOkAs(response, Playbook.class);
    }

    /**
     * Archives a playbook.
     */
    public Playbook archivePlaybook(String playbookId) {
        log.info("Archiving playbook: {}", playbookId);
        Response response = post("/" + playbookId + "/archive");
        return getOkAs(response, Playbook.class);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PLAYBOOK STEPS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Adds a step to playbook.
     */
    public Playbook addStep(String playbookId, Playbook.PlaybookStep step) {
        log.info("Adding step to playbook: {}", playbookId);
        Response response = post("/" + playbookId + "/steps", step);
        return getOkAs(response, Playbook.class);
    }

    /**
     * Updates a step in playbook.
     */
    public Playbook updateStep(String playbookId, String stepId, Playbook.PlaybookStep step) {
        log.info("Updating step {} in playbook: {}", stepId, playbookId);
        Response response = put("/" + playbookId + "/steps/" + stepId, step);
        return getOkAs(response, Playbook.class);
    }

    /**
     * Removes a step from playbook.
     */
    public Playbook removeStep(String playbookId, String stepId) {
        log.info("Removing step {} from playbook: {}", stepId, playbookId);
        Response response = delete("/" + playbookId + "/steps/" + stepId);
        return getOkAs(response, Playbook.class);
    }

    /**
     * Reorders steps in playbook.
     */
    public Playbook reorderSteps(String playbookId, List<String> stepIds) {
        log.info("Reordering steps in playbook: {}", playbookId);
        Response response = put("/" + playbookId + "/steps/order", Map.of(
            "stepIds", stepIds
        ));
        return getOkAs(response, Playbook.class);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PLAYBOOK VARIABLES
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Sets a variable in playbook.
     */
    public Playbook setVariable(String playbookId, Playbook.Variable variable) {
        log.info("Setting variable {} in playbook: {}", variable.getName(), playbookId);
        Response response = put("/" + playbookId + "/variables/" + variable.getName(), variable);
        return getOkAs(response, Playbook.class);
    }

    /**
     * Removes a variable from playbook.
     */
    public Playbook removeVariable(String playbookId, String variableName) {
        log.info("Removing variable {} from playbook: {}", variableName, playbookId);
        Response response = delete("/" + playbookId + "/variables/" + variableName);
        return getOkAs(response, Playbook.class);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PLAYBOOK SHARING
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Shares playbook with users.
     */
    public Playbook shareWith(String playbookId, List<String> userIds) {
        log.info("Sharing playbook {} with users: {}", playbookId, userIds);
        Response response = post("/" + playbookId + "/share", Map.of(
            "userIds", userIds
        ));
        return getOkAs(response, Playbook.class);
    }

    /**
     * Makes playbook public.
     */
    public Playbook makePublic(String playbookId) {
        log.info("Making playbook public: {}", playbookId);
        Response response = post("/" + playbookId + "/make-public");
        return getOkAs(response, Playbook.class);
    }

    /**
     * Makes playbook private.
     */
    public Playbook makePrivate(String playbookId) {
        log.info("Making playbook private: {}", playbookId);
        Response response = post("/" + playbookId + "/make-private");
        return getOkAs(response, Playbook.class);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PLAYBOOK TEMPLATES
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Gets available playbook templates.
     */
    public List<PlaybookTemplate> getTemplates() {
        log.info("Getting playbook templates");
        Response response = get("/templates");
        return List.of(getOkAs(response, PlaybookTemplate[].class));
    }

    /**
     * Gets a specific template.
     */
    public PlaybookTemplate getTemplate(String templateId) {
        log.info("Getting template: {}", templateId);
        Response response = get("/templates/" + templateId);
        return getOkAs(response, PlaybookTemplate.class);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PLAYBOOK VALIDATION
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Validates a playbook configuration.
     */
    public ValidationResult validatePlaybook(String playbookId) {
        log.info("Validating playbook: {}", playbookId);
        Response response = post("/" + playbookId + "/validate");
        return getOkAs(response, ValidationResult.class);
    }

    /**
     * Dry runs a playbook without actually executing.
     */
    public DryRunResult dryRun(String playbookId, Map<String, Object> variables) {
        log.info("Dry running playbook: {}", playbookId);
        Response response = post("/" + playbookId + "/dry-run", Map.of(
            "variables", variables
        ));
        return getOkAs(response, DryRunResult.class);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // HELPER CLASSES
    // ═══════════════════════════════════════════════════════════════════════════

    public static class PlaybookTemplate {
        public String id;
        public String name;
        public String description;
        public String category;
        public List<Playbook.PlaybookStep> steps;
    }

    public static class ValidationResult {
        public boolean valid;
        public List<ValidationError> errors;
        public List<String> warnings;

        public boolean isValid() {
            return valid;
        }
    }

    public static class ValidationError {
        public String stepId;
        public String field;
        public String message;
    }

    public static class DryRunResult {
        public boolean success;
        public List<StepPreview> stepPreviews;
        public String errorMessage;
    }

    public static class StepPreview {
        public String stepId;
        public String stepName;
        public Map<String, Object> resolvedConfig;
        public boolean wouldExecute;
    }
}
