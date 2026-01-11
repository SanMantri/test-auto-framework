package com.framework.core.browser;

import com.framework.core.config.FrameworkConfig;
import com.microsoft.playwright.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.file.Paths;

/**
 * BrowserManager - Manages Playwright browser lifecycle
 *
 * Single browser instance shared across all tests (for efficiency).
 * Each test gets its own isolated BrowserContext.
 *
 * Browser is launched once in @BeforeSuite and closed in @AfterSuite.
 */
@Slf4j
@Component
public class BrowserManager {

    @Autowired
    private FrameworkConfig config;

    private Playwright playwright;
    private Browser browser;

    // ═══════════════════════════════════════════════════════════════════════════
    // LIFECYCLE
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Initialize Playwright and launch browser.
     * Called once in @BeforeSuite.
     */
    public synchronized void initialize() {
        if (browser != null) {
            log.warn("Browser already initialized");
            return;
        }

        log.info("═══════════════════════════════════════════════════════════════");
        log.info("Initializing Playwright Browser");
        log.info("Browser Type: {}", config.getBrowserType());
        log.info("Headless: {}", config.isHeadless());
        log.info("Viewport: {}x{}", config.getViewportWidth(), config.getViewportHeight());
        log.info("═══════════════════════════════════════════════════════════════");

        playwright = Playwright.create();

        BrowserType.LaunchOptions options = new BrowserType.LaunchOptions()
            .setHeadless(config.isHeadless())
            .setSlowMo(config.getSlowMo());

        // Add any additional launch arguments
        // options.setArgs(List.of("--disable-dev-shm-usage"));

        browser = switch (config.getBrowserType().toLowerCase()) {
            case "firefox" -> playwright.firefox().launch(options);
            case "webkit" -> playwright.webkit().launch(options);
            default -> playwright.chromium().launch(options);
        };

        log.info("Browser initialized successfully");
    }

    /**
     * Close browser and Playwright.
     * Called in @AfterSuite.
     */
    public synchronized void close() {
        log.info("Closing browser...");

        if (browser != null) {
            browser.close();
            browser = null;
        }

        if (playwright != null) {
            playwright.close();
            playwright = null;
        }

        log.info("Browser closed successfully");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CONTEXT CREATION
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Creates a new browser context with optional authentication state.
     *
     * @param storageStatePath Path to storage state JSON (cookies, localStorage), or null for guest
     * @return New isolated BrowserContext
     */
    public BrowserContext createContext(String storageStatePath) {
        ensureBrowserInitialized();

        Browser.NewContextOptions options = new Browser.NewContextOptions()
            .setViewportSize(config.getViewportWidth(), config.getViewportHeight())
            .setIgnoreHTTPSErrors(true)
            .setJavaScriptEnabled(true)
            .setLocale("en-US")
            .setTimezoneId("America/New_York");

        // Inject authentication state if provided
        if (storageStatePath != null) {
            options.setStorageStatePath(Paths.get(storageStatePath));
            log.debug("Creating context with auth state from: {}", storageStatePath);
        } else {
            log.debug("Creating guest context (no auth state)");
        }

        BrowserContext context = browser.newContext(options);

        // Enable tracing if configured
        if (config.isTracingEnabled()) {
            context.tracing().start(new Tracing.StartOptions()
                .setScreenshots(true)
                .setSnapshots(true)
                .setSources(true));
            log.debug("Tracing enabled for context");
        }

        // Set default timeout
        context.setDefaultTimeout(config.getBrowser().getDefaultTimeout());

        return context;
    }

    /**
     * Creates a guest context (no authentication).
     */
    public BrowserContext createGuestContext() {
        return createContext(null);
    }

    /**
     * Creates a context with custom options.
     */
    public BrowserContext createContext(Browser.NewContextOptions options) {
        ensureBrowserInitialized();
        return browser.newContext(options);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // UTILITIES
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Gets the browser version.
     */
    public String getBrowserVersion() {
        ensureBrowserInitialized();
        return browser.version();
    }

    /**
     * Checks if browser is initialized.
     */
    public boolean isInitialized() {
        return browser != null && browser.isConnected();
    }

    /**
     * Gets number of open contexts.
     */
    public int getContextCount() {
        ensureBrowserInitialized();
        return browser.contexts().size();
    }

    private void ensureBrowserInitialized() {
        if (browser == null) {
            throw new IllegalStateException(
                "Browser not initialized. Call initialize() first (typically in @BeforeSuite)");
        }
    }
}
