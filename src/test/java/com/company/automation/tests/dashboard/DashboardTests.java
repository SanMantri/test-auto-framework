package com.company.automation.tests.dashboard;

import com.company.automation.core.agent.AgenticBrowser;
import com.company.automation.pages.dashboard.AnalyticsDashboardPage;
import com.company.automation.tests.BaseTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.Assert;
import org.testng.annotations.Test;

public class DashboardTests extends BaseTest {

    @Autowired
    private AnalyticsDashboardPage dashboardPage;

    @Autowired
    private AgenticBrowser agent;

    @Test(groups = { "visual" })
    public void verifyRevenueChart() {
        page.navigate("https://admin-dashboard.com/analytics");

        // Traditional Check: Is the element there?
        Assert.assertTrue(dashboardPage.verifyChartRendered(page));

        // Agentic Check: What does the page actually "look" like to an AI?
        AgenticBrowser.AgentObservation obs = agent.observe(page);
        System.out.println("AI Observation Title: " + obs.title);

        // In a real implementation:
        // boolean looksCorrect = openAI.ask("Does this chart look broken?",
        // obs.screenshotBase64);
        // Assert.assertTrue(looksCorrect);
    }
}
