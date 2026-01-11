package com.company.automation.core.agent;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.ScreenshotType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Agentic Browser Wrapper for AI/LLM-driven testing.
 * 
 * **Council Review Fixes Applied:**
 * - AI-01: Added `getAccessibilityTree()` to extract interactive elements.
 * - AI-03: Converted `AgentObservation` to an immutable Java record.
 */
@Component
public class AgenticBrowser {

    private static final Logger log = LoggerFactory.getLogger(AgenticBrowser.class);

    /**
     * Captures the current state of the page for an LLM.
     * Combines URL, Title, Accessibility Tree, and a Visual Snapshot.
     */
    public AgentObservation observe(Page page) {
        String url = page.url();
        String title = page.title();

        // AI-01: Get structured accessibility tree instead of raw innerText
        List<InteractiveElement> elements = getAccessibilityTree(page);

        // Visual Snapshot (for Vision models like Claude, Gemini Pro Vision)
        byte[] screenshotBytes = page.screenshot(new Page.ScreenshotOptions()
                .setType(ScreenshotType.JPEG)
                .setQuality(60)
                .setFullPage(false));
        String base64Image = Base64.getEncoder().encodeToString(screenshotBytes);

        log.debug("Observed page: '{}' at {}. Found {} interactive elements.", title, url, elements.size());
        return new AgentObservation(url, title, elements, base64Image);
    }

    /**
     * Extracts a structured list of all interactive elements on the page.
     * This gives the LLM a "map" of what it can click, type, or select.
     */
    public List<InteractiveElement> getAccessibilityTree(Page page) {
        List<InteractiveElement> elements = new ArrayList<>();

        // Buttons
        addElements(elements, page.getByRole(AriaRole.BUTTON), "BUTTON");
        // Links
        addElements(elements, page.getByRole(AriaRole.LINK), "LINK");
        // Text Inputs
        addElements(elements, page.getByRole(AriaRole.TEXTBOX), "TEXTBOX");
        // Checkboxes
        addElements(elements, page.getByRole(AriaRole.CHECKBOX), "CHECKBOX");
        // Radio Buttons
        addElements(elements, page.getByRole(AriaRole.RADIO), "RADIO");
        // Menus/Items
        addElements(elements, page.getByRole(AriaRole.MENUITEM), "MENUITEM");
        // Tabs
        addElements(elements, page.getByRole(AriaRole.TAB), "TAB");

        return elements;
    }

    private void addElements(List<InteractiveElement> list, Locator locator, String role) {
        int count = locator.count();
        for (int i = 0; i < count; i++) {
            try {
                Locator element = locator.nth(i);
                String name = element.getAttribute("aria-label");
                if (name == null || name.isEmpty()) {
                    name = element.innerText().lines().findFirst().orElse("").trim();
                }
                if (name.length() > 50) {
                    name = name.substring(0, 50) + "...";
                }
                // Generate a stable, unique selector for the LLM to use
                String selector = String.format("getByRole('%s').nth(%d)", role.toLowerCase(), i);
                list.add(new InteractiveElement(role, name, selector));
            } catch (Exception e) {
                // Element might be stale, skip it
            }
        }
    }

    /**
     * Executes an AI-generated action on the page.
     */
    public void executeAction(Page page, AgentAction action) {
        log.info("🤖 Agent executing: {} on '{}'", action.type(), action.target());
        switch (action.type().toUpperCase()) {
            case "CLICK" -> page.locator(action.target()).click();
            case "TYPE" -> page.locator(action.target()).fill(action.data());
            case "NAVIGATE" -> page.navigate(action.data());
            case "SELECT" -> page.locator(action.target()).selectOption(action.data());
            case "HOVER" -> page.locator(action.target()).hover();
            case "SCROLL" -> page.locator(action.target()).scrollIntoViewIfNeeded();
            case "WAIT" -> page.waitForTimeout(Long.parseLong(action.data()));
            default -> log.warn("Unknown action type: {}", action.type());
        }
    }

    // --- IMMUTABLE DATA MODELS (AI-03) ---

    /**
     * Immutable observation record for the LLM.
     */
    public record AgentObservation(
            String url,
            String title,
            List<InteractiveElement> interactiveElements,
            String screenshotBase64) {
    }

    /**
     * Represents a single interactive element on the page.
     */
    public record InteractiveElement(
            String role, // e.g., BUTTON, LINK, TEXTBOX
            String name, // Visible text or aria-label
            String selector // Playwright selector to target this element
    ) {
    }

    /**
     * Represents an action for the agent to execute.
     */
    public record AgentAction(
            String type, // CLICK, TYPE, NAVIGATE, SELECT, HOVER, SCROLL, WAIT
            String target, // Selector or URL
            String data // Optional data (text to type, option to select)
    ) {
    }
}
