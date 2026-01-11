package com.framework.core.auth;

import com.framework.core.config.FrameworkConfig;
import com.microsoft.playwright.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumMap;
import java.util.Map;

/**
 * AuthenticationManager - Login once, reuse everywhere
 *
 * Philosophy:
 * - Authenticate each user role ONCE during @BeforeSuite
 * - Store browser state (cookies, localStorage) to disk
 * - Inject stored state into new browser contexts (instant auth)
 *
 * Result: 100 tests with login = 5 seconds total (not 25 minutes)
 */
@Slf4j
@Component
public class AuthenticationManager {

    @Autowired
    private FrameworkConfig config;

    private final Map<UserRole, String> storageStatePaths = new EnumMap<>(UserRole.class);
    private final Map<UserRole, String> authTokens = new EnumMap<>(UserRole.class);

    private static final Path AUTH_DIR = Paths.get("target", "auth");

    // ═══════════════════════════════════════════════════════════════════════════
    // USER ROLES
    // ═══════════════════════════════════════════════════════════════════════════

    public enum UserRole {
        ADMIN,
        STANDARD_USER,
        GUEST
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SUITE-LEVEL AUTHENTICATION
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Authenticates all configured user roles.
     * Call this once in @BeforeSuite.
     */
    public void authenticateAllRoles() {
        log.info("═══════════════════════════════════════════════════════════════");
        log.info("Authenticating all user roles");
        log.info("═══════════════════════════════════════════════════════════════");

        try {
            Files.createDirectories(AUTH_DIR);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create auth directory", e);
        }

        // Authenticate each role (except GUEST)
        for (UserRole role : UserRole.values()) {
            if (role != UserRole.GUEST) {
                try {
                    authenticateRole(role);
                    log.info("✓ {} authenticated successfully", role);
                } catch (Exception e) {
                    log.error("✗ Failed to authenticate {}: {}", role, e.getMessage());
                    throw new RuntimeException("Authentication failed for " + role, e);
                }
            }
        }

        log.info("═══════════════════════════════════════════════════════════════");
        log.info("All roles authenticated");
        log.info("═══════════════════════════════════════════════════════════════");
    }

    /**
     * Authenticates a single role using API + Browser state capture.
     */
    private void authenticateRole(UserRole role) {
        log.info("Authenticating role: {}", role);

        Credentials creds = getCredentials(role);
        if (creds == null) {
            throw new IllegalArgumentException("No credentials configured for role: " + role);
        }

        // Option 1: API-based authentication (faster, preferred)
        if (supportsApiAuth()) {
            authenticateViaApi(role, creds);
        }

        // Option 2: Browser-based authentication (fallback)
        authenticateViaBrowser(role, creds);
    }

    /**
     * API-based authentication - fastest approach.
     * Gets auth token via API, then injects into browser state.
     */
    private void authenticateViaApi(UserRole role, Credentials creds) {
        log.debug("Attempting API authentication for {}", role);

        try {
            Response response = RestAssured.given()
                .baseUri(config.getApiUrl())
                .contentType("application/json")
                .body(Map.of(
                    "email", creds.username(),
                    "password", creds.password()
                ))
                .post("/auth/login");

            if (response.statusCode() == 200) {
                String token = response.jsonPath().getString("token");
                authTokens.put(role, token);
                log.debug("API auth successful for {}, token obtained", role);
            }
        } catch (Exception e) {
            log.debug("API auth not available, falling back to browser auth: {}", e.getMessage());
        }
    }

    /**
     * Browser-based authentication - captures full browser state.
     */
    private void authenticateViaBrowser(UserRole role, Credentials creds) {
        log.debug("Browser authentication for {}", role);

        Playwright playwright = Playwright.create();
        Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
            .setHeadless(config.isHeadless()));

        BrowserContext context = browser.newContext();
        Page page = context.newPage();

        try {
            // Navigate to login page
            page.navigate(config.getBaseUrl() + "/login");
            page.waitForLoadState();

            // Fill login form
            // Adjust selectors based on your actual login page
            page.fill("[data-testid='email-input'], #email, input[type='email'], input[name='email']",
                creds.username());
            page.fill("[data-testid='password-input'], #password, input[type='password'], input[name='password']",
                creds.password());

            // Submit login
            page.click("[data-testid='login-button'], button[type='submit'], #login-btn");

            // Wait for successful login (redirect away from login page)
            page.waitForURL(url -> !url.contains("/login"),
                new Page.WaitForURLOptions().setTimeout(15000));

            // Wait for page to fully load
            page.waitForLoadState(LoadState.NETWORKIDLE);

            // Save storage state
            Path statePath = AUTH_DIR.resolve(role.name().toLowerCase() + "-state.json");
            context.storageState(new BrowserContext.StorageStateOptions().setPath(statePath));
            storageStatePaths.put(role, statePath.toString());

            log.debug("Browser state saved for {} to {}", role, statePath);

        } catch (Exception e) {
            // Capture screenshot on failure
            try {
                byte[] screenshot = page.screenshot();
                Files.write(AUTH_DIR.resolve(role.name().toLowerCase() + "-auth-failure.png"), screenshot);
            } catch (IOException ignored) {}

            throw new RuntimeException("Browser authentication failed for " + role, e);
        } finally {
            context.close();
            browser.close();
            playwright.close();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // STATE RETRIEVAL
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Gets the storage state path for a role.
     * Used by BrowserManager to create authenticated contexts.
     */
    public String getStorageState(UserRole role) {
        if (role == UserRole.GUEST) {
            return null;
        }

        String path = storageStatePaths.get(role);
        if (path == null) {
            throw new IllegalStateException(
                "No storage state found for " + role + ". Was authenticateAllRoles() called?");
        }
        return path;
    }

    /**
     * Gets the auth token for a role (for API calls).
     */
    public String getAuthToken(UserRole role) {
        return authTokens.get(role);
    }

    /**
     * Checks if a role has been authenticated.
     */
    public boolean isAuthenticated(UserRole role) {
        return storageStatePaths.containsKey(role) || authTokens.containsKey(role);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CREDENTIALS
    // ═══════════════════════════════════════════════════════════════════════════

    private Credentials getCredentials(UserRole role) {
        return switch (role) {
            case ADMIN -> new Credentials(
                config.getAdminUsername(),
                config.getAdminPassword()
            );
            case STANDARD_USER -> new Credentials(
                config.getUserUsername(),
                config.getUserPassword()
            );
            case GUEST -> null;
        };
    }

    private boolean supportsApiAuth() {
        // Check if API auth endpoint exists
        try {
            Response response = RestAssured.given()
                .baseUri(config.getApiUrl())
                .get("/auth/login");
            return response.statusCode() != 404;
        } catch (Exception e) {
            return false;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // INNER CLASSES
    // ═══════════════════════════════════════════════════════════════════════════

    private record Credentials(String username, String password) {}
}
