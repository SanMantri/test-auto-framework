package com.company.automation.tests;

import com.company.automation.core.context.ContextManager;
import com.microsoft.playwright.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.ITestResult;
import org.testng.annotations.*;

/**
 * Base class for all Playwright tests.
 * 
 * **Council Review Fixes Applied:**
 * - ARCH-01: Moved auth init to @BeforeClass with alwaysRun=true for thread
 * safety.
 * - PERF-02: closeContext now in @AfterMethod with alwaysRun=true to guarantee
 * cleanup.
 * - Added test result tracking for conditional trace saving.
 */
@SpringBootTest(classes = com.company.automation.AutomationApplication.class)
public class BaseTest extends AbstractTestNGSpringContextTests {

    private static final Logger log = LoggerFactory.getLogger(BaseTest.class);

    // Static flag to ensure auth is only generated once across all parallel classes
    private static volatile boolean authStateInitialized = false;
    private static final Object authLock = new Object();

    @Autowired
    protected ContextManager contextManager;

    protected Page page;

    /**
     * Thread-safe, lazy initialization of the global auth state.
     * Runs once before any test class, regardless of parallelism.
     */
    @BeforeClass(alwaysRun = true)
    public void setupAuth() {
        if (!authStateInitialized) {
            synchronized (authLock) {
                if (!authStateInitialized) {
                    log.info("🔐 Initializing Global Auth State (once per suite)...");
                    contextManager.generateGlobalAuthState();
                    authStateInitialized = true;
                }
            }
        }
    }

    /**
     * Creates a fresh, isolated BrowserContext for each test method.
     */
    @BeforeMethod(alwaysRun = true)
    public void setupTest(ITestResult result) {
        String testName = result.getMethod().getMethodName();
        log.info("🧪 Setting up test: {}", testName);

        var context = contextManager.createContext(testName);
        page = context.newPage();
    }

    /**
     * Tears down the context, saving traces.
     * Uses alwaysRun=true to guarantee execution even if the test fails.
     */
    @AfterMethod(alwaysRun = true)
    public void tearDownTest(ITestResult result) {
        boolean passed = result.getStatus() == ITestResult.SUCCESS;
        String testName = result.getMethod().getMethodName();
        log.info("{} Test finished: {} (passed={})", passed ? "✅" : "❌", testName, passed);

        contextManager.closeContext(passed);
    }
}
