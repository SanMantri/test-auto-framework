package com.company.automation.core.driver;

import com.microsoft.playwright.*;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Thread-Safe Singleton Factory to manage the Playwright instance and Browser.
 * 
 * **Council Review Fixes Applied:**
 * - SEC-03: Replaced System.out.println with SLF4J Logger.
 * - ARCH-02: Migrated from javax.annotation to jakarta.annotation.
 * - ARCH-03: Externalized headless mode to application.properties.
 * - PERF-01: Added synchronized double-checked locking for thread safety.
 */
@Component
public class PlaywrightFactory {

    private static final Logger log = LoggerFactory.getLogger(PlaywrightFactory.class);

    private volatile Playwright playwright;
    private volatile Browser browser;

    // Externalized config: controlled via application.properties
    @Value("${automation.headless:true}")
    private boolean headless;

    @Value("${automation.browser.channel:chrome}")
    private String browserChannel; // e.g., "chrome", "msedge", "chromium"

    @PostConstruct
    public synchronized void init() {
        if (playwright == null) {
            log.info("Initializing Playwright Factory (headless={}, channel={})...", headless, browserChannel);
            playwright = Playwright.create();
            browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                    .setChannel(browserChannel)
                    .setHeadless(headless)
                    .setArgs(List.of("--disable-infobars", "--disable-extensions")));
            log.info("✅ Browser Launched Successfully.");
        }
    }

    /**
     * Thread-safe browser access with double-checked locking.
     */
    public Browser getBrowser() {
        if (browser == null || !browser.isConnected()) {
            synchronized (this) {
                if (browser == null || !browser.isConnected()) {
                    init();
                }
            }
        }
        return browser;
    }

    @PreDestroy
    public synchronized void tearDown() {
        log.info("Shutting down Playwright Factory...");
        if (browser != null) {
            browser.close();
            browser = null;
        }
        if (playwright != null) {
            playwright.close();
            playwright = null;
        }
        log.info("✅ Playwright Factory Shut Down.");
    }
}
