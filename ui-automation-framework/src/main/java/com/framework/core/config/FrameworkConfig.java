package com.framework.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * FrameworkConfig - Central configuration for the test framework
 *
 * Loaded from application.yml with prefix "framework"
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "framework")
public class FrameworkConfig {

    private String environment = "dev";
    private String baseUrl = "http://localhost:3000";
    private String apiUrl = "http://localhost:8080";

    private BrowserConfig browser = new BrowserConfig();
    private AuthConfig auth = new AuthConfig();
    private ParallelConfig parallel = new ParallelConfig();
    private ReportingConfig reporting = new ReportingConfig();

    @Data
    public static class BrowserConfig {
        private String type = "chromium";
        private boolean headless = true;
        private int slowMo = 0;
        private ViewportConfig viewport = new ViewportConfig();
        private boolean tracingEnabled = true;
        private int defaultTimeout = 30000;

        @Data
        public static class ViewportConfig {
            private int width = 1920;
            private int height = 1080;
        }
    }

    @Data
    public static class AuthConfig {
        private UserCredentials admin = new UserCredentials();
        private UserCredentials user = new UserCredentials();

        @Data
        public static class UserCredentials {
            private String username;
            private String password;
        }
    }

    @Data
    public static class ParallelConfig {
        private int threadCount = 5;
        private int timeoutSeconds = 30;
    }

    @Data
    public static class ReportingConfig {
        private boolean screenshotsOnFailure = true;
        private boolean networkLogs = true;
        private boolean consoleLogs = true;
    }

    // Convenience methods
    public String getBrowserType() {
        return browser.getType();
    }

    public boolean isHeadless() {
        return browser.isHeadless();
    }

    public int getSlowMo() {
        return browser.getSlowMo();
    }

    public int getViewportWidth() {
        return browser.getViewport().getWidth();
    }

    public int getViewportHeight() {
        return browser.getViewport().getHeight();
    }

    public boolean isTracingEnabled() {
        return browser.isTracingEnabled();
    }

    public String getAdminUsername() {
        return auth.getAdmin().getUsername();
    }

    public String getAdminPassword() {
        return auth.getAdmin().getPassword();
    }

    public String getUserUsername() {
        return auth.getUser().getUsername();
    }

    public String getUserPassword() {
        return auth.getUser().getPassword();
    }
}
