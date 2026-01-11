package com.framework.core.base;

import com.framework.core.auth.AuthenticationManager;
import com.framework.core.auth.AuthenticationManager.UserRole;
import com.framework.core.browser.BrowserManager;
import com.framework.core.config.FrameworkConfig;
import com.framework.core.data.GlobalDataCache;
import com.framework.core.data.TestDataCache;
import com.framework.core.reporting.NetworkLogger;
import com.microsoft.playwright.*;
import io.qameta.allure.Allure;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.ITestResult;
import org.testng.annotations.*;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * BaseTest - Foundation for all UI tests
 *
 * Provides:
 * - Browser/context management per test
 * - Authentication state injection
 * - Test data cache isolation
 * - Screenshot on failure
 * - Network/console logging
 *
 * Lifecycle:
 * - @BeforeSuite: Initialize browser, authenticate all roles
 * - @BeforeMethod: Create isolated context with auth, clear test data
 * - @AfterMethod: Capture artifacts on failure, close context
 * - @AfterSuite: Close browser
 */
@Slf4j
@SpringBootTest
public abstract class BaseTest extends AbstractTestNGSpringContextTests {

    // ═══════════════════════════════════════════════════════════════════════════
    // INJECTED DEPENDENCIES
    // ═══════════════════════════════════════════════════════════════════════════

    @Autowired
    protected FrameworkConfig config;

    @Autowired
    protected BrowserManager browserManager;

    @Autowired
    protected AuthenticationManager authManager;

    @Autowired
    protected GlobalDataCache globalDataCache;

    @Autowired
    protected ApplicationContext applicationContext;

    // ═══════════════════════════════════════════════════════════════════════════
    // THREAD-LOCAL INSTANCES (Isolated per test thread)
    // ═══════════════════════════════════════════════════════════════════════════

    private static final ThreadLocal<BrowserContext> contextHolder = new ThreadLocal<>();
    private static final ThreadLocal<Page> pageHolder = new ThreadLocal<>();
    private static final ThreadLocal<TestDataCache> testDataHolder = new ThreadLocal<>();
    private static final ThreadLocal<NetworkLogger> networkLoggerHolder = new ThreadLocal<>();
    private static final ThreadLocal<String> currentTestNameHolder = new ThreadLocal<>();

    // ═══════════════════════════════════════════════════════════════════════════
    // ACCESSORS
    // ═══════════════════════════════════════════════════════════════════════════

    protected BrowserContext context() {
        return contextHolder.get();
    }

    protected Page page() {
        return pageHolder.get();
    }

    protected TestDataCache testDataCache() {
        return testDataHolder.get();
    }

    protected NetworkLogger networkLogger() {
        return networkLoggerHolder.get();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SUITE LIFECYCLE
    // ═══════════════════════════════════════════════════════════════════════════

    @BeforeSuite(alwaysRun = true)
    public void beforeSuite() {
        log.info("╔═══════════════════════════════════════════════════════════════╗");
        log.info("║              STARTING TEST SUITE                              ║");
        log.info("╠═══════════════════════════════════════════════════════════════╣");
        log.info("║ Environment : {}                                        ", config.getEnvironment());
        log.info("║ Base URL    : {}                              ", config.getBaseUrl());
        log.info("║ Browser     : {}                                    ", config.getBrowserType());
        log.info("║ Headless    : {}                                         ", config.isHeadless());
        log.info("╚═══════════════════════════════════════════════════════════════╝");

        // Initialize browser
        browserManager.initialize();

        // Authenticate all user roles
        authManager.authenticateAllRoles();

        // Store common data in global cache
        globalDataCache.put("baseUrl", config.getBaseUrl());
        globalDataCache.put("apiUrl", config.getApiUrl());
        globalDataCache.put("environment", config.getEnvironment());
    }

    @AfterSuite(alwaysRun = true)
    public void afterSuite() {
        log.info("╔═══════════════════════════════════════════════════════════════╗");
        log.info("║              COMPLETING TEST SUITE                            ║");
        log.info("╚═══════════════════════════════════════════════════════════════╝");

        // Close browser
        browserManager.close();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TEST LIFECYCLE
    // ═══════════════════════════════════════════════════════════════════════════

    @BeforeMethod(alwaysRun = true)
    public void beforeMethod(ITestResult result) {
        String testName = result.getMethod().getMethodName();
        currentTestNameHolder.set(testName);

        log.info("┌───────────────────────────────────────────────────────────────┐");
        log.info("│ TEST: {}                                          ", testName);
        log.info("└───────────────────────────────────────────────────────────────┘");

        // Create isolated test data cache
        testDataHolder.set(new TestDataCache());

        // Create new browser context with auth state
        String storageStatePath = authManager.getStorageState(getRequiredRole());
        BrowserContext context = browserManager.createContext(storageStatePath);
        contextHolder.set(context);

        // Create new page
        Page page = context.newPage();
        pageHolder.set(page);

        // Setup network logging
        NetworkLogger networkLogger = new NetworkLogger();
        networkLogger.attachToPage(page);
        networkLoggerHolder.set(networkLogger);

        // Setup console logging
        page.onConsoleMessage(msg -> {
            if ("error".equals(msg.type())) {
                log.warn("Console Error: {}", msg.text());
            }
        });

        // Start tracing if enabled
        if (config.isTracingEnabled()) {
            context.tracing().start(new Tracing.StartOptions()
                .setScreenshots(true)
                .setSnapshots(true)
                .setSources(true));
        }

        log.debug("Test setup complete - context and page ready");
    }

    @AfterMethod(alwaysRun = true)
    public void afterMethod(ITestResult result) {
        String testName = currentTestNameHolder.get();
        boolean passed = result.getStatus() == ITestResult.SUCCESS;

        try {
            // Capture artifacts on failure
            if (!passed) {
                captureFailureArtifacts(testName, result);
            }

            // Stop and save trace
            if (config.isTracingEnabled() && context() != null) {
                Path tracePath = Paths.get("target", "traces", testName + ".zip");
                Files.createDirectories(tracePath.getParent());
                context().tracing().stop(new Tracing.StopOptions().setPath(tracePath));

                if (!passed) {
                    Allure.addAttachment("Trace", "application/zip",
                        Files.newInputStream(tracePath), ".zip");
                }
            }

            // Attach network log
            if (networkLoggerHolder.get() != null) {
                networkLoggerHolder.get().attachToAllureReport();
            }

        } catch (Exception e) {
            log.error("Error in afterMethod: {}", e.getMessage());
        } finally {
            // Close context
            if (context() != null) {
                context().close();
            }

            // Clear thread-local data
            contextHolder.remove();
            pageHolder.remove();
            testDataHolder.remove();
            networkLoggerHolder.remove();
            currentTestNameHolder.remove();
        }

        log.info("┌───────────────────────────────────────────────────────────────┐");
        log.info("│ RESULT: {} - {}                                       ", testName, passed ? "PASS ✓" : "FAIL ✗");
        log.info("└───────────────────────────────────────────────────────────────┘");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ROLE-BASED AUTHENTICATION
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Override to specify required role for the test class.
     * Default is STANDARD_USER.
     */
    protected UserRole getRequiredRole() {
        return UserRole.STANDARD_USER;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // UTILITY METHODS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Get Spring bean (API client, playbook, etc.)
     */
    protected <T> T getBean(Class<T> beanClass) {
        return applicationContext.getBean(beanClass);
    }

    /**
     * Create page object with current page
     */
    protected <T extends BasePage> T getPage(Class<T> pageClass) {
        try {
            return pageClass.getConstructor(Page.class).newInstance(page());
        } catch (Exception e) {
            throw new RuntimeException("Failed to create page object: " + pageClass, e);
        }
    }

    /**
     * Create page object with current page and base URL
     */
    protected <T extends BasePage> T getPage(Class<T> pageClass, String baseUrl) {
        try {
            return pageClass.getConstructor(Page.class, String.class).newInstance(page(), baseUrl);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create page object: " + pageClass, e);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // FAILURE HANDLING
    // ═══════════════════════════════════════════════════════════════════════════

    private void captureFailureArtifacts(String testName, ITestResult result) {
        log.info("Capturing failure artifacts for: {}", testName);

        try {
            // Screenshot
            if (page() != null) {
                byte[] screenshot = page().screenshot(new Page.ScreenshotOptions().setFullPage(true));
                Allure.addAttachment("Screenshot - " + testName, "image/png",
                    new ByteArrayInputStream(screenshot), ".png");

                // Save to file
                Path screenshotPath = Paths.get("target", "screenshots", testName + ".png");
                Files.createDirectories(screenshotPath.getParent());
                Files.write(screenshotPath, screenshot);

                // Page URL
                Allure.addAttachment("URL", "text/plain", page().url());

                // Page HTML
                String html = page().content();
                Allure.addAttachment("Page HTML", "text/html", html);
            }

            // Exception
            if (result.getThrowable() != null) {
                Allure.addAttachment("Exception", "text/plain",
                    result.getThrowable().toString());
            }

            // Test data cache contents
            if (testDataCache() != null && !testDataCache().isEmpty()) {
                Allure.addAttachment("Test Data Cache", "text/plain",
                    testDataCache().getAll().toString());
            }

        } catch (Exception e) {
            log.error("Failed to capture failure artifacts: {}", e.getMessage());
        }
    }
}
