package com.company.automation.core.agent;

/**
 * Interface for LLM Client providers.
 * Implement this interface to connect to different AI models (Claude, Gemini,
 * OpenAI, Local LLMs).
 * 
 * **Council Review Item:** AI-02 (TO-DO for Next Phase)
 * This interface defines the contract for the JourneyDiscoverer to get AI
 * decisions.
 */
public interface LlmClient {

    /**
     * Sends an observation to the LLM and gets the next action to perform.
     *
     * @param observation The current state of the page (URL, title, elements,
     *                    screenshot).
     * @param goal        The high-level goal of the exploration (e.g., "Add a
     *                    product to the cart").
     * @return The next action the agent should take.
     */
    AgenticBrowser.AgentAction decide(AgenticBrowser.AgentObservation observation, String goal);

    /**
     * Generates test cases based on the discovered journey.
     *
     * @param journeyHistory A list of observations and actions taken during
     *                       exploration.
     * @return Generated test code as a string.
     */
    String generateTestCode(java.util.List<AgenticBrowser.AgentObservation> journeyHistory);
}
