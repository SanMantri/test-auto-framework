package com.company.automation.core.agent.discovery;

import com.company.automation.core.agent.AgenticBrowser;
import com.company.automation.core.agent.AgenticBrowser.AgentAction;
import com.company.automation.core.agent.AgenticBrowser.AgentObservation;
import com.company.automation.core.agent.LlmClient;
import com.microsoft.playwright.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Service that drives "Agentic Exploration" of the application.
 * Uses an LLM to decide what to do next based on what it "sees".
 * 
 * **Council Review Fixes Applied:**
 * - Now uses the `LlmClient` interface (AI-02).
 * - Contains a fallback "Stub" LLM for offline/local development.
 */
@Service
public class JourneyDiscoverer {

    private static final Logger log = LoggerFactory.getLogger(JourneyDiscoverer.class);

    @Autowired
    private AgenticBrowser agent;

    // The LLM client can be injected (or null for stub mode)
    @Autowired(required = false)
    private LlmClient llmClient;

    @Value("${automation.agent.max-steps:15}")
    private int maxSteps;

    /**
     * Explores the application starting from the current page state.
     * The agent will observe, decide, and act until the goal is reached or max
     * steps exceeded.
     *
     * @param page The Playwright page to explore.
     * @param goal The high-level goal (e.g., "Complete a purchase").
     * @return A history of observations made during the journey.
     */
    public List<AgentObservation> explore(Page page, String goal) {
        log.info("🚀 Starting Agentic Exploration for goal: '{}'", goal);
        List<AgentObservation> history = new ArrayList<>();

        for (int step = 1; step <= maxSteps; step++) {
            log.info("--- Step {}/{} ---", step, maxSteps);

            // 1. OBSERVE: What does the agent see?
            AgentObservation obs = agent.observe(page);
            history.add(obs);
            log.info("👁️ Agent sees: '{}' at {} with {} interactive elements.",
                    obs.title(), obs.url(), obs.interactiveElements().size());

            // 2. DECIDE: What should the agent do next?
            AgentAction action = decideNextAction(obs, goal);

            if (action == null || "DONE".equalsIgnoreCase(action.type())) {
                log.info("🎯 Goal achieved or agent determined exploration complete.");
                break;
            }

            // 3. ACT: Execute the decision
            try {
                agent.executeAction(page, action);
                page.waitForLoadState(); // Wait for navigation/AJAX
            } catch (Exception e) {
                log.warn("⚠️ Action failed: {}. Continuing exploration.", e.getMessage());
            }
        }

        log.info("🏁 Exploration finished. Total steps: {}", history.size());
        return history;
    }

    /**
     * Decides the next action. Uses the LlmClient if available, otherwise falls
     * back to a stub.
     */
    private AgentAction decideNextAction(AgentObservation obs, String goal) {
        if (llmClient != null) {
            log.debug("Using LLM Client for decision...");
            return llmClient.decide(obs, goal);
        } else {
            log.debug("LLM Client not available. Using stub logic.");
            return stubDecision(obs, goal);
        }
    }

    /**
     * A simple stub decision engine for offline development.
     * Replace with actual LLM integration for production.
     */
    private AgentAction stubDecision(AgentObservation obs, String goal) {
        // Example: If the goal mentions "login" and we see a login button, click it.
        for (var el : obs.interactiveElements()) {
            String name = el.name().toLowerCase();
            String goalLower = goal.toLowerCase();

            if (goalLower.contains("login") && name.contains("login")) {
                return new AgentAction("CLICK", el.selector(), null);
            }
            if (goalLower.contains("cart") && name.contains("cart")) {
                return new AgentAction("CLICK", el.selector(), null);
            }
            if (goalLower.contains("buy") && name.contains("buy")) {
                return new AgentAction("CLICK", el.selector(), null);
            }
        }
        // If no matching element found, signal exploration is done.
        return new AgentAction("DONE", null, null);
    }
}
