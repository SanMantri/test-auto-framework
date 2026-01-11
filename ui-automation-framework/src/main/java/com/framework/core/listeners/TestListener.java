package com.framework.core.listeners;

import io.qameta.allure.Allure;
import lombok.extern.slf4j.Slf4j;
import org.testng.*;

/**
 * TestListener - Custom TestNG listener for test lifecycle events
 *
 * Provides:
 * - Test timing
 * - Retry logging
 * - Allure enhancements
 */
@Slf4j
public class TestListener implements ITestListener, ISuiteListener {

    // ═══════════════════════════════════════════════════════════════════════════
    // SUITE EVENTS
    // ═══════════════════════════════════════════════════════════════════════════

    @Override
    public void onStart(ISuite suite) {
        log.info("╔═══════════════════════════════════════════════════════════════╗");
        log.info("║ Suite Started: {}", suite.getName());
        log.info("╚═══════════════════════════════════════════════════════════════╝");
    }

    @Override
    public void onFinish(ISuite suite) {
        log.info("╔═══════════════════════════════════════════════════════════════╗");
        log.info("║ Suite Finished: {}", suite.getName());
        log.info("╚═══════════════════════════════════════════════════════════════╝");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TEST EVENTS
    // ═══════════════════════════════════════════════════════════════════════════

    @Override
    public void onTestStart(ITestResult result) {
        String testName = getTestName(result);
        log.info("▶ Test Started: {}", testName);

        // Add test info to Allure
        Allure.getLifecycle().updateTestCase(testCase -> {
            testCase.setName(testName);
        });
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        long duration = result.getEndMillis() - result.getStartMillis();
        log.info("✓ Test Passed: {} ({}ms)", getTestName(result), duration);
    }

    @Override
    public void onTestFailure(ITestResult result) {
        long duration = result.getEndMillis() - result.getStartMillis();
        log.error("✗ Test Failed: {} ({}ms)", getTestName(result), duration);

        Throwable throwable = result.getThrowable();
        if (throwable != null) {
            log.error("  Error: {}", throwable.getMessage());
        }
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        log.warn("⊘ Test Skipped: {}", getTestName(result));

        Throwable throwable = result.getThrowable();
        if (throwable != null) {
            log.warn("  Reason: {}", throwable.getMessage());
        }
    }

    @Override
    public void onTestFailedButWithinSuccessPercentage(ITestResult result) {
        log.warn("~ Test Failed (within success %): {}", getTestName(result));
    }

    @Override
    public void onTestFailedWithTimeout(ITestResult result) {
        log.error("⏱ Test Timed Out: {}", getTestName(result));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════════════════

    private String getTestName(ITestResult result) {
        return result.getTestClass().getRealClass().getSimpleName() + "." +
               result.getMethod().getMethodName();
    }
}
