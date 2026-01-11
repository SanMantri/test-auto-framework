package com.company.automation.core.context;

import com.company.automation.core.driver.PlaywrightFactory;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Tracing;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Manages BrowserContexts and handles the "Single Login" strategy.
 * 
 * **Council Review Fixes Applied:**
 * - SEC-01: Auth state path is now configurable and defaults to `target/auth/`.
 * - ARCH-02: Migrated to jakarta.annotation.
 * - ARCH-04: Added Playwright Tracing support.
 * - PERF-02: Improved ThreadLocal cleanup with guaranteed removal.
 * - PERF-03: Made video recording conditional via config.
 */
@Component
public class ContextManager {

    private static final Logger log = LoggerFactory.getLogger(ContextManager.class);

    @Autowired
    private PlaywrightFactory playwrightFactory;

    // Configurable path for storage state (gitignored by default in target/)
    @Value("${automation.auth.storage-state-path:target/auth/auth_state.json}")
    private String storageStatePath;

    @Value("${automation.recording.video.enabled:false}")
    private boolean videoEnabled;

    @Value("${automation.recording.video.dir:target/videos/}")
    private String videoDir;

    @Value("${automation.tracing.enabled:true}")
    private boolean tracingEnabled;

    @Value("${automation.tracing.dir:target/traces/}")
    private String tracingDir;

    // ThreadLocal to ensure each test thread gets its own isolated context
    private final ThreadLocal<BrowserContext> threadContext = new ThreadLocal<>();
    private final ThreadLocal<String> threadTestName = new ThreadLocal<>();

    @PostConstruct
    public void initDirectories() throws IOException {
        // Ensure auth directory exists
        Path authPath = Paths.get(storageStatePath).getParent();
        if (authPath != null && !Files.exists(authPath)) {
            Files.createDirectories(authPath);
            log.info("Created auth directory: {}", authPath);
        }
        if (tracingEnabled) {
            Files.createDirectories(Paths.get(tracingDir));
        }
        if (videoEnabled) {
            Files.createDirectories(Paths.get(videoDir));
        }
    }

    /**
     * Called ONCE per Suite (preferably via Spring lifecycle or a static setup).
     * Performs login (API or UI) and saves the browser state.
     */
    public void generateGlobalAuthState() {
        log.info("Generating Global Auth State...");
        try (BrowserContext context = playwrightFactory.getBrowser().newContext()) {
            // --- HOOK: Implement your actual login logic here ---
            // Option A: API Login (Recommended)
            // Call your login API, get session cookies, inject them:
            // context.addCookies(List.of(cookieFromApi));
            //
            // Option B: One-time UI Login
            // Page loginPage = context.newPage();
            // loginPage.navigate("https://your-app.com/login");
            // loginPage.fill("#username", "your_user");
            // loginPage.fill("#password", "your_pass");
            // loginPage.click("#login-btn");
            // loginPage.waitForURL("**/dashboard");
            // ---------------------------------------------------

            // Save state to the configured, secure path
            context.storageState(new BrowserContext.StorageStateOptions()
                    .setPath(Paths.get(storageStatePath)));
            log.info("✅ Global Auth State saved to: {}", storageStatePath);
        } catch (Exception e) {
            log.error("❌ Failed to generate global auth state: {}", e.getMessage(), e);
            throw new RuntimeException("Auth state generation failed", e);
        }
    }

    /**
     * Provisions a NEW, ISOLATED context for a test.
     * Injects auth state and optionally enables tracing and video.
     * 
     * @param testName A unique name for this test (used for trace/video filenames).
     */
    public BrowserContext createContext(String testName) {
        log.info("Provisioning BrowserContext for test: '{}' on thread: {}", testName, Thread.currentThread().getId());
        Browser browser = playwrightFactory.getBrowser();

        Browser.NewContextOptions options = new Browser.NewContextOptions()
                .setStorageStatePath(Paths.get(storageStatePath))
                .setViewportSize(1920, 1080);

        if (videoEnabled) {
            options.setRecordVideoDir(Paths.get(videoDir));
        }

        BrowserContext context = browser.newContext(options);

        // --- ARCH-04: Start Tracing ---
        if (tracingEnabled) {
            context.tracing().start(new Tracing.StartOptions()
                    .setScreenshots(true)
                    .setSnapshots(true)
                    .setSources(false)); // Set to true for source code in trace
            log.debug("Tracing started for test: {}", testName);
        }

        threadContext.set(context);
        threadTestName.set(testName);
        return context;
    }

    public BrowserContext getCurrentContext() {
        return threadContext.get();
    }

    /**
     * Closes the context and saves trace/video.
     * Uses try-finally to guarantee ThreadLocal cleanup.
     */
    public void closeContext(boolean testPassed) {
        BrowserContext context = threadContext.get();
        String testName = threadTestName.get();
        try {
            if (context != null) {
                // --- ARCH-04: Stop Tracing ---
                if (tracingEnabled) {
                    Path tracePath = Paths.get(tracingDir, testName + ".zip");
                    context.tracing().stop(new Tracing.StopOptions().setPath(tracePath));
                    log.info("📼 Trace saved: {}", tracePath);
                }
                context.close();
                log.debug("Context closed for test: {}", testName);
            }
        } finally {
            // --- PERF-02: GUARANTEED cleanup ---
            threadContext.remove();
            threadTestName.remove();
        }
    }
}
