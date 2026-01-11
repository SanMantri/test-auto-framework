# Directory Structure & Code Examples

## Document Information
| Attribute | Value |
|-----------|-------|
| Version | 1.0 |
| Scope | Complete Framework |
| Stack | Java 17, SpringBoot 3.x, TestNG, Playwright, Maven |

---

## 1. Complete Directory Structure

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                          PROJECT DIRECTORY STRUCTURE                                     │
├─────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                          │
│  ui-automation-framework/                                                                │
│  │                                                                                       │
│  ├── pom.xml                                # Maven configuration                        │
│  ├── testng.xml                             # Master test suite                          │
│  ├── README.md                              # Project documentation                      │
│  │                                                                                       │
│  ├── src/                                                                                │
│  │   ├── main/                                                                          │
│  │   │   ├── java/                                                                      │
│  │   │   │   └── com/                                                                   │
│  │   │   │       └── framework/                                                         │
│  │   │   │           │                                                                  │
│  │   │   │           ├── core/              # CORE INFRASTRUCTURE                       │
│  │   │   │           │   ├── config/                                                    │
│  │   │   │           │   │   ├── FrameworkConfig.java                                   │
│  │   │   │           │   │   ├── BrowserConfig.java                                     │
│  │   │   │           │   │   └── EnvironmentConfig.java                                 │
│  │   │   │           │   │                                                              │
│  │   │   │           │   ├── browser/                                                   │
│  │   │   │           │   │   ├── BrowserManager.java                                    │
│  │   │   │           │   │   ├── ContextPool.java                                       │
│  │   │   │           │   │   └── PlaywrightFactory.java                                 │
│  │   │   │           │   │                                                              │
│  │   │   │           │   ├── auth/                                                      │
│  │   │   │           │   │   ├── AuthenticationManager.java                             │
│  │   │   │           │   │   ├── StorageStateManager.java                               │
│  │   │   │           │   │   └── UserRole.java                                          │
│  │   │   │           │   │                                                              │
│  │   │   │           │   ├── data/                                                      │
│  │   │   │           │   │   ├── GlobalDataCache.java                                   │
│  │   │   │           │   │   ├── TestDataCache.java                                     │
│  │   │   │           │   │   └── DataProvider.java                                      │
│  │   │   │           │   │                                                              │
│  │   │   │           │   ├── base/                                                      │
│  │   │   │           │   │   ├── BasePage.java                                          │
│  │   │   │           │   │   ├── BaseTest.java                                          │
│  │   │   │           │   │   └── BaseApiClient.java                                     │
│  │   │   │           │   │                                                              │
│  │   │   │           │   ├── reporting/                                                 │
│  │   │   │           │   │   ├── AllureReporter.java                                    │
│  │   │   │           │   │   ├── NetworkLogger.java                                     │
│  │   │   │           │   │   └── ConsoleLogger.java                                     │
│  │   │   │           │   │                                                              │
│  │   │   │           │   ├── listeners/                                                 │
│  │   │   │           │   │   ├── TestListener.java                                      │
│  │   │   │           │   │   ├── RetryAnalyzer.java                                     │
│  │   │   │           │   │   └── ScreenshotListener.java                                │
│  │   │   │           │   │                                                              │
│  │   │   │           │   └── utils/                                                     │
│  │   │   │           │       ├── WaitUtils.java                                         │
│  │   │   │           │       ├── JsonUtils.java                                         │
│  │   │   │           │       └── FileUtils.java                                         │
│  │   │   │           │                                                                  │
│  │   │   │           ├── domains/           # DOMAIN MODULES                            │
│  │   │   │           │   │                                                              │
│  │   │   │           │   ├── payments/      # PAYMENTS DOMAIN                           │
│  │   │   │           │   │   ├── api/                                                   │
│  │   │   │           │   │   │   ├── CartApiClient.java                                 │
│  │   │   │           │   │   │   ├── CheckoutApiClient.java                             │
│  │   │   │           │   │   │   ├── OrderApiClient.java                                │
│  │   │   │           │   │   │   └── PaymentApiClient.java                              │
│  │   │   │           │   │   ├── pages/                                                 │
│  │   │   │           │   │   │   ├── CartPage.java                                      │
│  │   │   │           │   │   │   ├── CheckoutPage.java                                  │
│  │   │   │           │   │   │   ├── PaymentPage.java                                   │
│  │   │   │           │   │   │   └── OrderConfirmationPage.java                         │
│  │   │   │           │   │   ├── models/                                                │
│  │   │   │           │   │   │   ├── Cart.java                                          │
│  │   │   │           │   │   │   ├── Order.java                                         │
│  │   │   │           │   │   │   └── PaymentMethod.java                                 │
│  │   │   │           │   │   └── playbooks/                                             │
│  │   │   │           │   │       ├── CartSetupPlaybook.java                             │
│  │   │   │           │   │       └── CheckoutPlaybook.java                              │
│  │   │   │           │   │                                                              │
│  │   │   │           │   ├── booking/       # BOOKING DOMAIN                            │
│  │   │   │           │   │   ├── api/                                                   │
│  │   │   │           │   │   │   ├── MovieApiClient.java                                │
│  │   │   │           │   │   │   ├── SeatApiClient.java                                 │
│  │   │   │           │   │   │   └── BookingApiClient.java                              │
│  │   │   │           │   │   ├── pages/                                                 │
│  │   │   │           │   │   │   ├── MovieListPage.java                                 │
│  │   │   │           │   │   │   ├── SeatSelectionPage.java                             │
│  │   │   │           │   │   │   └── TicketPage.java                                    │
│  │   │   │           │   │   ├── models/                                                │
│  │   │   │           │   │   │   ├── Movie.java                                         │
│  │   │   │           │   │   │   ├── Seat.java                                          │
│  │   │   │           │   │   │   └── Booking.java                                       │
│  │   │   │           │   │   └── playbooks/                                             │
│  │   │   │           │   │       ├── ShowFinderPlaybook.java                            │
│  │   │   │           │   │       └── BookingPlaybook.java                               │
│  │   │   │           │   │                                                              │
│  │   │   │           │   ├── playbook/      # PLAYBOOK DOMAIN                           │
│  │   │   │           │   │   ├── api/                                                   │
│  │   │   │           │   │   │   ├── PlaybookApiClient.java                             │
│  │   │   │           │   │   │   └── ExecutionApiClient.java                            │
│  │   │   │           │   │   ├── pages/                                                 │
│  │   │   │           │   │   │   ├── PlaybookBuilderPage.java                           │
│  │   │   │           │   │   │   └── ExecutionHistoryPage.java                          │
│  │   │   │           │   │   ├── models/                                                │
│  │   │   │           │   │   │   ├── Playbook.java                                      │
│  │   │   │           │   │   │   └── Execution.java                                     │
│  │   │   │           │   │   └── mocks/                                                 │
│  │   │   │           │   │       ├── MockSlackServer.java                               │
│  │   │   │           │   │       └── MockEmailServer.java                               │
│  │   │   │           │   │                                                              │
│  │   │   │           │   └── dashboard/     # DASHBOARD DOMAIN                          │
│  │   │   │           │       ├── api/                                                   │
│  │   │   │           │       │   ├── DashboardApiClient.java                            │
│  │   │   │           │       │   └── MetricsApiClient.java                              │
│  │   │   │           │       ├── pages/                                                 │
│  │   │   │           │       │   └── DashboardViewPage.java                             │
│  │   │   │           │       ├── components/                                            │
│  │   │   │           │       │   ├── KPICardComponent.java                              │
│  │   │   │           │       │   ├── LineChartComponent.java                            │
│  │   │   │           │       │   └── DataTableComponent.java                            │
│  │   │   │           │       └── visual/                                                │
│  │   │   │           │           └── ChartScreenshotHelper.java                         │
│  │   │   │           │                                                                  │
│  │   │   │           └── Application.java   # SpringBoot entry                          │
│  │   │   │                                                                              │
│  │   │   └── resources/                                                                 │
│  │   │       ├── application.yml            # Default config                            │
│  │   │       ├── application-dev.yml        # Dev environment                           │
│  │   │       ├── application-staging.yml    # Staging environment                       │
│  │   │       └── application-prod.yml       # Prod environment                          │
│  │   │                                                                                  │
│  │   └── test/                                                                          │
│  │       ├── java/                                                                      │
│  │       │   └── com/                                                                   │
│  │       │       └── framework/                                                         │
│  │       │           └── tests/             # TEST CLASSES                              │
│  │       │               │                                                              │
│  │       │               ├── payments/                                                  │
│  │       │               │   ├── CartTests.java                                         │
│  │       │               │   ├── CheckoutTests.java                                     │
│  │       │               │   ├── PaymentTests.java                                      │
│  │       │               │   └── E2EPaymentJourneyTests.java                            │
│  │       │               │                                                              │
│  │       │               ├── booking/                                                   │
│  │       │               │   ├── SeatSelectionTests.java                                │
│  │       │               │   ├── BookingFlowTests.java                                  │
│  │       │               │   └── ConcurrencyTests.java                                  │
│  │       │               │                                                              │
│  │       │               ├── playbook/                                                  │
│  │       │               │   ├── PlaybookBuilderTests.java                              │
│  │       │               │   └── PlaybookExecutionTests.java                            │
│  │       │               │                                                              │
│  │       │               └── dashboard/                                                 │
│  │       │                   ├── KPIAccuracyTests.java                                  │
│  │       │                   ├── ChartDataAccuracyTests.java                            │
│  │       │                   └── VisualRegressionTests.java                             │
│  │       │                                                                              │
│  │       └── resources/                                                                 │
│  │           ├── testdata/                  # Test data files                           │
│  │           │   ├── payments/                                                          │
│  │           │   │   ├── test-cards.json                                                │
│  │           │   │   └── addresses.json                                                 │
│  │           │   ├── booking/                                                           │
│  │           │   │   └── seat-configs.json                                              │
│  │           │   └── dashboard/                                                         │
│  │           │       └── test-metrics.json                                              │
│  │           │                                                                          │
│  │           ├── visual-baselines/          # Screenshot baselines                      │
│  │           │   └── dashboard/                                                         │
│  │           │       └── *.png                                                          │
│  │           │                                                                          │
│  │           └── suites/                    # TestNG suites                             │
│  │               ├── payments-suite.xml                                                 │
│  │               ├── booking-suite.xml                                                  │
│  │               ├── playbook-suite.xml                                                 │
│  │               └── dashboard-suite.xml                                                │
│  │                                                                                      │
│  ├── target/                                # Build output                              │
│  │   ├── allure-results/                    # Raw Allure data                           │
│  │   ├── allure-report/                     # Generated HTML report                     │
│  │   ├── screenshots/                       # Failure screenshots                       │
│  │   └── traces/                            # Playwright traces                         │
│  │                                                                                      │
│  └── .github/                               # CI/CD                                     │
│      └── workflows/                                                                     │
│          ├── ci.yml                         # Main CI pipeline                          │
│          └── nightly.yml                    # Nightly regression                        │
│                                                                                          │
└─────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Core Infrastructure Code

### 2.1 pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">

    <modelVersion>4.0.0</modelVersion>

    <groupId>com.company</groupId>
    <artifactId>ui-automation-framework</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>jar</packaging>

    <name>UI Automation Framework</name>
    <description>Hybrid API-UI Test Automation Framework</description>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.0</version>
        <relativePath/>
    </parent>

    <properties>
        <java.version>17</java.version>
        <playwright.version>1.40.0</playwright.version>
        <testng.version>7.8.0</testng.version>
        <rest-assured.version>5.4.0</rest-assured.version>
        <allure.version>2.25.0</allure.version>
        <assertj.version>3.24.2</assertj.version>
        <lombok.version>1.18.30</lombok.version>

        <!-- Test execution -->
        <parallel.tests>5</parallel.tests>
        <thread.count>5</thread.count>
    </properties>

    <dependencies>
        <!-- Spring Boot -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>

        <!-- Playwright -->
        <dependency>
            <groupId>com.microsoft.playwright</groupId>
            <artifactId>playwright</artifactId>
            <version>${playwright.version}</version>
        </dependency>

        <!-- TestNG -->
        <dependency>
            <groupId>org.testng</groupId>
            <artifactId>testng</artifactId>
            <version>${testng.version}</version>
        </dependency>

        <!-- REST Assured -->
        <dependency>
            <groupId>io.rest-assured</groupId>
            <artifactId>rest-assured</artifactId>
            <version>${rest-assured.version}</version>
        </dependency>

        <!-- Allure -->
        <dependency>
            <groupId>io.qameta.allure</groupId>
            <artifactId>allure-testng</artifactId>
            <version>${allure.version}</version>
        </dependency>

        <!-- AssertJ -->
        <dependency>
            <groupId>org.assertj</groupId>
            <artifactId>assertj-core</artifactId>
            <version>${assertj.version}</version>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <version>${lombok.version}</version>
            <scope>provided</scope>
        </dependency>

        <!-- Jackson for JSON -->
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
        </dependency>

        <!-- SLF4J + Logback -->
        <dependency>
            <groupId>ch.qos.logback</groupId>
            <artifactId>logback-classic</artifactId>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <!-- Maven Surefire for TestNG -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>3.2.2</version>
                <configuration>
                    <suiteXmlFiles>
                        <suiteXmlFile>testng.xml</suiteXmlFile>
                    </suiteXmlFiles>
                    <parallel>methods</parallel>
                    <threadCount>${thread.count}</threadCount>
                    <argLine>
                        -javaagent:"${settings.localRepository}/org/aspectj/aspectjweaver/1.9.20.1/aspectjweaver-1.9.20.1.jar"
                    </argLine>
                    <systemPropertyVariables>
                        <allure.results.directory>target/allure-results</allure.results.directory>
                    </systemPropertyVariables>
                </configuration>
                <dependencies>
                    <dependency>
                        <groupId>org.aspectj</groupId>
                        <artifactId>aspectjweaver</artifactId>
                        <version>1.9.20.1</version>
                    </dependency>
                </dependencies>
            </plugin>

            <!-- Allure Report -->
            <plugin>
                <groupId>io.qameta.allure</groupId>
                <artifactId>allure-maven</artifactId>
                <version>2.12.0</version>
                <configuration>
                    <reportVersion>${allure.version}</reportVersion>
                </configuration>
            </plugin>
        </plugins>
    </build>

    <profiles>
        <!-- Environment Profiles -->
        <profile>
            <id>dev</id>
            <properties>
                <spring.profiles.active>dev</spring.profiles.active>
            </properties>
        </profile>
        <profile>
            <id>staging</id>
            <properties>
                <spring.profiles.active>staging</spring.profiles.active>
            </properties>
        </profile>
        <profile>
            <id>prod</id>
            <properties>
                <spring.profiles.active>prod</spring.profiles.active>
            </properties>
        </profile>

        <!-- Domain Profiles -->
        <profile>
            <id>payments</id>
            <build>
                <plugins>
                    <plugin>
                        <groupId>org.apache.maven.plugins</groupId>
                        <artifactId>maven-surefire-plugin</artifactId>
                        <configuration>
                            <suiteXmlFiles>
                                <suiteXmlFile>src/test/resources/suites/payments-suite.xml</suiteXmlFile>
                            </suiteXmlFiles>
                        </configuration>
                    </plugin>
                </plugins>
            </build>
        </profile>
    </profiles>
</project>
```

### 2.2 BaseTest.java

```java
package com.framework.core.base;

import com.framework.core.auth.AuthenticationManager;
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
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.ITestResult;
import org.testng.annotations.*;

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

    // ═══════════════════════════════════════════════════════════════════════════
    // THREAD-LOCAL INSTANCES (Per Test)
    // ═══════════════════════════════════════════════════════════════════════════

    protected static ThreadLocal<BrowserContext> contextHolder = new ThreadLocal<>();
    protected static ThreadLocal<Page> pageHolder = new ThreadLocal<>();
    protected static ThreadLocal<TestDataCache> testDataHolder = new ThreadLocal<>();
    protected static ThreadLocal<NetworkLogger> networkLoggerHolder = new ThreadLocal<>();

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

    // ═══════════════════════════════════════════════════════════════════════════
    // SUITE LIFECYCLE
    // ═══════════════════════════════════════════════════════════════════════════

    @BeforeSuite(alwaysRun = true)
    public void beforeSuite() {
        log.info("═══════════════════════════════════════════════════════════════");
        log.info("Starting Test Suite");
        log.info("Environment: {}", config.getEnvironment());
        log.info("Base URL: {}", config.getBaseUrl());
        log.info("═══════════════════════════════════════════════════════════════");

        // Initialize browser once
        browserManager.initialize();

        // Authenticate once and store state
        authManager.authenticateAllRoles();

        // Store common data
        globalDataCache.put("baseUrl", config.getBaseUrl());
    }

    @AfterSuite(alwaysRun = true)
    public void afterSuite() {
        log.info("═══════════════════════════════════════════════════════════════");
        log.info("Completing Test Suite");
        log.info("═══════════════════════════════════════════════════════════════");

        // Close browser
        browserManager.close();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TEST LIFECYCLE
    // ═══════════════════════════════════════════════════════════════════════════

    @BeforeMethod(alwaysRun = true)
    public void beforeMethod() {
        log.info("────────────────────────────────────────────────────────────────");
        log.info("Starting Test");
        log.info("────────────────────────────────────────────────────────────────");

        // Create isolated test data cache
        testDataHolder.set(new TestDataCache());

        // Create new browser context with auth state
        BrowserContext context = browserManager.createContext(
            authManager.getStorageState(getRequiredRole())
        );
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
            if (msg.type().equals("error")) {
                log.warn("Console Error: {}", msg.text());
            }
        });
    }

    @AfterMethod(alwaysRun = true)
    public void afterMethod(ITestResult result) {
        try {
            // Capture artifacts on failure
            if (result.getStatus() == ITestResult.FAILURE) {
                captureFailureArtifacts(result);
            }

            // Attach network log
            if (networkLoggerHolder.get() != null) {
                networkLoggerHolder.get().attachToAllureReport();
            }

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
        }

        log.info("────────────────────────────────────────────────────────────────");
        log.info("Test Complete: {}", result.getStatus() == ITestResult.SUCCESS ? "PASS" : "FAIL");
        log.info("────────────────────────────────────────────────────────────────");
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

    public enum UserRole {
        ADMIN,
        STANDARD_USER,
        GUEST
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // UTILITY METHODS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Get API client with auto-injection from Spring context
     */
    protected <T> T getApi(Class<T> apiClientClass) {
        return applicationContext.getBean(apiClientClass);
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

    // ═══════════════════════════════════════════════════════════════════════════
    // FAILURE HANDLING
    // ═══════════════════════════════════════════════════════════════════════════

    private void captureFailureArtifacts(ITestResult result) {
        String testName = result.getMethod().getMethodName();

        try {
            // Screenshot
            byte[] screenshot = page().screenshot(new Page.ScreenshotOptions()
                .setFullPage(true));
            Allure.addAttachment("Screenshot - " + testName, "image/png",
                new java.io.ByteArrayInputStream(screenshot), ".png");

            // Save to file as well
            java.nio.file.Files.write(
                Paths.get("target/screenshots", testName + ".png"),
                screenshot
            );

            // Trace
            context().tracing().stop(new Tracing.StopOptions()
                .setPath(Paths.get("target/traces", testName + ".zip")));

            // Page HTML
            String html = page().content();
            Allure.addAttachment("Page HTML", "text/html", html);

            // Current URL
            Allure.addAttachment("URL", "text/plain", page().url());

        } catch (Exception e) {
            log.error("Failed to capture failure artifacts", e);
        }
    }
}
```

### 2.3 BasePage.java

```java
package com.framework.core.base;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitForSelectorState;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * BasePage - Foundation for all Page Objects
 *
 * Provides:
 * - Common wait methods
 * - Element interaction utilities
 * - Screenshot helpers
 * - Standard locator patterns
 */
@Slf4j
public abstract class BasePage {

    protected final Page page;
    protected final String baseUrl;

    // ═══════════════════════════════════════════════════════════════════════════
    // CONSTRUCTOR
    // ═══════════════════════════════════════════════════════════════════════════

    public BasePage(Page page) {
        this.page = page;
        this.baseUrl = System.getProperty("baseUrl", "http://localhost:3000");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ABSTRACT METHODS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Override to define how to wait for page to be ready
     */
    protected abstract void waitForPageLoad();

    // ═══════════════════════════════════════════════════════════════════════════
    // WAIT UTILITIES
    // ═══════════════════════════════════════════════════════════════════════════

    protected void waitForVisible(String selector) {
        page.waitForSelector(selector,
            new Page.WaitForSelectorOptions().setState(WaitForSelectorState.VISIBLE));
    }

    protected void waitForHidden(String selector) {
        page.waitForSelector(selector,
            new Page.WaitForSelectorOptions().setState(WaitForSelectorState.HIDDEN));
    }

    protected void waitForEnabled(String selector) {
        page.waitForSelector(selector + ":not([disabled])");
    }

    protected void waitForNetworkIdle() {
        page.waitForLoadState(LoadState.NETWORKIDLE);
    }

    protected void waitForText(String selector, String text) {
        page.waitForFunction(
            String.format("document.querySelector('%s')?.textContent?.includes('%s')",
                selector, text)
        );
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ELEMENT INTERACTION
    // ═══════════════════════════════════════════════════════════════════════════

    protected void click(String selector) {
        page.click(selector);
    }

    protected void clickAndWait(String selector, String waitForSelector) {
        page.click(selector);
        waitForVisible(waitForSelector);
    }

    protected void fill(String selector, String value) {
        page.fill(selector, value);
    }

    protected void clearAndFill(String selector, String value) {
        page.locator(selector).clear();
        page.fill(selector, value);
    }

    protected void selectOption(String selector, String value) {
        page.selectOption(selector, value);
    }

    protected void check(String selector) {
        page.check(selector);
    }

    protected void uncheck(String selector) {
        page.uncheck(selector);
    }

    protected String getText(String selector) {
        return page.locator(selector).textContent();
    }

    protected String getValue(String selector) {
        return page.inputValue(selector);
    }

    protected boolean isVisible(String selector) {
        return page.locator(selector).isVisible();
    }

    protected boolean isEnabled(String selector) {
        return page.locator(selector).isEnabled();
    }

    protected boolean isChecked(String selector) {
        return page.locator(selector).isChecked();
    }

    protected int count(String selector) {
        return page.locator(selector).count();
    }

    protected List<String> getAllTexts(String selector) {
        return page.locator(selector).allTextContents();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // NAVIGATION
    // ═══════════════════════════════════════════════════════════════════════════

    protected void navigateTo(String path) {
        page.navigate(baseUrl + path);
        waitForPageLoad();
    }

    protected void reload() {
        page.reload();
        waitForPageLoad();
    }

    protected String getCurrentUrl() {
        return page.url();
    }

    protected String getTitle() {
        return page.title();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SCREENSHOTS
    // ═══════════════════════════════════════════════════════════════════════════

    protected byte[] captureScreenshot() {
        return page.screenshot();
    }

    protected byte[] captureFullPageScreenshot() {
        return page.screenshot(new Page.ScreenshotOptions().setFullPage(true));
    }

    protected byte[] captureElementScreenshot(String selector) {
        return page.locator(selector).screenshot();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // KEYBOARD & MOUSE
    // ═══════════════════════════════════════════════════════════════════════════

    protected void pressKey(String key) {
        page.keyboard().press(key);
    }

    protected void hover(String selector) {
        page.hover(selector);
    }

    protected void doubleClick(String selector) {
        page.dblclick(selector);
    }

    protected void rightClick(String selector) {
        page.click(selector, new Page.ClickOptions().setButton(MouseButton.RIGHT));
    }

    protected void dragAndDrop(String source, String target) {
        page.dragAndDrop(source, target);
    }
}
```

### 2.4 TestDataCache.java

```java
package com.framework.core.data;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * TestDataCache - Thread-isolated test data storage
 *
 * Each test thread gets its own instance via ThreadLocal in BaseTest.
 * Data is automatically cleared after each test.
 */
@Slf4j
public class TestDataCache {

    private final Map<String, Object> cache = new HashMap<>();

    // ═══════════════════════════════════════════════════════════════════════════
    // CORE OPERATIONS
    // ═══════════════════════════════════════════════════════════════════════════

    public void put(String key, Object value) {
        cache.put(key, value);
        log.debug("TestDataCache: Stored {} = {}", key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) cache.get(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key, T defaultValue) {
        return (T) cache.getOrDefault(key, defaultValue);
    }

    public boolean containsKey(String key) {
        return cache.containsKey(key);
    }

    public void remove(String key) {
        cache.remove(key);
    }

    public void clear() {
        cache.clear();
        log.debug("TestDataCache: Cleared all data");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CONVENIENCE METHODS
    // ═══════════════════════════════════════════════════════════════════════════

    public String getString(String key) {
        return get(key);
    }

    public Integer getInteger(String key) {
        return get(key);
    }

    public Long getLong(String key) {
        return get(key);
    }

    public Boolean getBoolean(String key) {
        return get(key);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CHAINING SUPPORT
    // ═══════════════════════════════════════════════════════════════════════════

    public TestDataCache set(String key, Object value) {
        put(key, value);
        return this;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DEBUG
    // ═══════════════════════════════════════════════════════════════════════════

    public void logContents() {
        log.info("TestDataCache contents:");
        cache.forEach((k, v) -> log.info("  {} = {}", k, v));
    }
}
```

### 2.5 GlobalDataCache.java

```java
package com.framework.core.data;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * GlobalDataCache - Suite-level shared data
 *
 * Thread-safe storage for data shared across all tests.
 * Typically contains:
 * - Configuration values
 * - Auth tokens
 * - Master data
 */
@Component
public class GlobalDataCache {

    private final ConcurrentHashMap<String, Object> cache = new ConcurrentHashMap<>();

    public void put(String key, Object value) {
        cache.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) cache.get(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key, T defaultValue) {
        return (T) cache.getOrDefault(key, defaultValue);
    }

    public boolean containsKey(String key) {
        return cache.containsKey(key);
    }

    public void clear() {
        cache.clear();
    }
}
```

### 2.6 AuthenticationManager.java

```java
package com.framework.core.auth;

import com.framework.core.base.BaseTest.UserRole;
import com.framework.core.config.FrameworkConfig;
import com.microsoft.playwright.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumMap;
import java.util.Map;

/**
 * AuthenticationManager - Handles login once, reuse everywhere
 *
 * Authenticates each role once during suite setup.
 * Stores browser state (cookies, localStorage) to disk.
 * Injects state into new browser contexts for instant auth.
 */
@Slf4j
@Component
public class AuthenticationManager {

    @Autowired
    private FrameworkConfig config;

    private final Map<UserRole, String> storageStatePaths = new EnumMap<>(UserRole.class);
    private Browser browser;

    // ═══════════════════════════════════════════════════════════════════════════
    // SUITE-LEVEL AUTHENTICATION
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Authenticates all roles and stores their state.
     * Called once in @BeforeSuite.
     */
    public void authenticateAllRoles() {
        log.info("Authenticating all user roles...");

        // Create temporary browser for login
        Playwright playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
            .setHeadless(config.isHeadless()));

        try {
            for (UserRole role : UserRole.values()) {
                if (role != UserRole.GUEST) {
                    authenticateRole(role);
                }
            }
        } finally {
            browser.close();
            playwright.close();
        }

        log.info("All roles authenticated successfully");
    }

    private void authenticateRole(UserRole role) {
        log.info("Authenticating role: {}", role);

        BrowserContext context = browser.newContext();
        Page page = context.newPage();

        try {
            // Navigate to login
            page.navigate(config.getBaseUrl() + "/login");

            // Get credentials for role
            Credentials creds = getCredentials(role);

            // Perform login
            page.fill("[data-testid='email-input']", creds.username());
            page.fill("[data-testid='password-input']", creds.password());
            page.click("[data-testid='login-button']");

            // Wait for login to complete
            page.waitForURL("**/dashboard**");

            // Save storage state
            Path statePath = Paths.get("target/auth", role.name().toLowerCase() + "-state.json");
            Files.createDirectories(statePath.getParent());
            context.storageState(new BrowserContext.StorageStateOptions()
                .setPath(statePath));

            storageStatePaths.put(role, statePath.toString());
            log.info("Saved auth state for {} to {}", role, statePath);

        } catch (Exception e) {
            log.error("Failed to authenticate role: {}", role, e);
            throw new RuntimeException("Authentication failed for " + role, e);
        } finally {
            context.close();
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
            return null; // No auth state for guest
        }

        String path = storageStatePaths.get(role);
        if (path == null) {
            throw new IllegalStateException(
                "No auth state found for " + role + ". Was authenticateAllRoles() called?");
        }
        return path;
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

    private record Credentials(String username, String password) {}
}
```

### 2.7 BrowserManager.java

```java
package com.framework.core.browser;

import com.framework.core.config.FrameworkConfig;
import com.microsoft.playwright.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.file.Paths;

/**
 * BrowserManager - Manages browser lifecycle
 *
 * Single browser instance shared across all tests.
 * Each test gets isolated context.
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

    public void initialize() {
        log.info("Initializing browser: {}", config.getBrowserType());

        playwright = Playwright.create();

        BrowserType.LaunchOptions options = new BrowserType.LaunchOptions()
            .setHeadless(config.isHeadless())
            .setSlowMo(config.getSlowMo());

        browser = switch (config.getBrowserType().toLowerCase()) {
            case "firefox" -> playwright.firefox().launch(options);
            case "webkit" -> playwright.webkit().launch(options);
            default -> playwright.chromium().launch(options);
        };

        log.info("Browser initialized successfully");
    }

    public void close() {
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
        log.info("Browser closed");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CONTEXT CREATION
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Creates new browser context with optional auth state
     */
    public BrowserContext createContext(String storageStatePath) {
        Browser.NewContextOptions options = new Browser.NewContextOptions()
            .setViewportSize(config.getViewportWidth(), config.getViewportHeight())
            .setIgnoreHTTPSErrors(true);

        // Inject auth state if provided
        if (storageStatePath != null) {
            options.setStorageStatePath(Paths.get(storageStatePath));
        }

        BrowserContext context = browser.newContext(options);

        // Enable tracing if configured
        if (config.isTracingEnabled()) {
            context.tracing().start(new Tracing.StartOptions()
                .setScreenshots(true)
                .setSnapshots(true)
                .setSources(true));
        }

        return context;
    }

    /**
     * Creates guest context (no auth)
     */
    public BrowserContext createGuestContext() {
        return createContext(null);
    }
}
```

---

## 3. TestNG Suite Configuration

### 3.1 Master testng.xml

```xml
<!DOCTYPE suite SYSTEM "https://testng.org/testng-1.0.dtd">
<suite name="Master Test Suite" parallel="suites" thread-count="4">

    <listeners>
        <listener class-name="io.qameta.allure.testng.AllureTestNg"/>
        <listener class-name="com.framework.core.listeners.TestListener"/>
        <listener class-name="com.framework.core.listeners.ScreenshotListener"/>
    </listeners>

    <!-- Global parameters -->
    <parameter name="browser" value="chromium"/>
    <parameter name="headless" value="true"/>

    <!-- Include domain suites -->
    <suite-files>
        <suite-file path="src/test/resources/suites/payments-suite.xml"/>
        <suite-file path="src/test/resources/suites/booking-suite.xml"/>
        <suite-file path="src/test/resources/suites/playbook-suite.xml"/>
        <suite-file path="src/test/resources/suites/dashboard-suite.xml"/>
    </suite-files>

</suite>
```

### 3.2 Domain Suite (payments-suite.xml)

```xml
<!DOCTYPE suite SYSTEM "https://testng.org/testng-1.0.dtd">
<suite name="Payments Test Suite" parallel="methods" thread-count="5">

    <listeners>
        <listener class-name="io.qameta.allure.testng.AllureTestNg"/>
    </listeners>

    <!-- Smoke Tests (Fast, Critical) -->
    <test name="Payments Smoke Tests">
        <groups>
            <run>
                <include name="smoke"/>
                <include name="payments"/>
            </run>
        </groups>
        <classes>
            <class name="com.framework.tests.payments.CheckoutTests"/>
            <class name="com.framework.tests.payments.PaymentTests"/>
        </classes>
    </test>

    <!-- API Tests (No Browser) -->
    <test name="Payments API Tests">
        <groups>
            <run>
                <include name="api"/>
                <include name="payments"/>
            </run>
        </groups>
        <classes>
            <class name="com.framework.tests.payments.CartTests"/>
        </classes>
    </test>

    <!-- E2E Tests -->
    <test name="Payments E2E Tests">
        <groups>
            <run>
                <include name="e2e"/>
                <include name="payments"/>
            </run>
        </groups>
        <classes>
            <class name="com.framework.tests.payments.E2EPaymentJourneyTests"/>
        </classes>
    </test>

</suite>
```

---

## 4. Configuration Files

### 4.1 application.yml

```yaml
# Default configuration
framework:
  environment: dev
  base-url: ${BASE_URL:http://localhost:3000}
  api-url: ${API_URL:http://localhost:8080}

  browser:
    type: chromium
    headless: true
    slow-mo: 0
    viewport:
      width: 1920
      height: 1080
    tracing-enabled: true

  auth:
    admin:
      username: ${ADMIN_USER:admin@test.com}
      password: ${ADMIN_PASS:admin123}
    user:
      username: ${USER_USER:user@test.com}
      password: ${USER_PASS:user123}

  parallel:
    thread-count: 5
    timeout-seconds: 30

  reporting:
    screenshots-on-failure: true
    network-logs: true
    console-logs: true

spring:
  main:
    banner-mode: off

logging:
  level:
    com.framework: DEBUG
    com.microsoft.playwright: WARN
```

### 4.2 application-staging.yml

```yaml
framework:
  environment: staging
  base-url: https://staging.example.com
  api-url: https://api.staging.example.com

  browser:
    headless: true
    slow-mo: 0
```

---

## 5. CI/CD Configuration

### 5.1 GitHub Actions (.github/workflows/ci.yml)

```yaml
name: UI Automation Tests

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]
  workflow_dispatch:
    inputs:
      suite:
        description: 'Test suite to run'
        required: false
        default: 'all'
        type: choice
        options:
          - all
          - payments
          - booking
          - playbook
          - dashboard

jobs:
  test:
    runs-on: ubuntu-latest

    strategy:
      fail-fast: false
      matrix:
        shard: [1, 2, 3, 4]

    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: 'maven'

      - name: Install Playwright
        run: mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install --with-deps chromium"

      - name: Run Tests (Shard ${{ matrix.shard }})
        run: |
          mvn test \
            -Dshard.index=${{ matrix.shard }} \
            -Dshard.total=4 \
            -Dspring.profiles.active=staging
        env:
          BASE_URL: ${{ secrets.STAGING_URL }}
          ADMIN_USER: ${{ secrets.ADMIN_USER }}
          ADMIN_PASS: ${{ secrets.ADMIN_PASS }}

      - name: Upload Allure Results
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: allure-results-${{ matrix.shard }}
          path: target/allure-results/
          retention-days: 5

      - name: Upload Screenshots
        if: failure()
        uses: actions/upload-artifact@v4
        with:
          name: screenshots-${{ matrix.shard }}
          path: target/screenshots/
          retention-days: 5

  report:
    needs: test
    if: always()
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v4

      - name: Download All Allure Results
        uses: actions/download-artifact@v4
        with:
          pattern: allure-results-*
          path: allure-results
          merge-multiple: true

      - name: Generate Allure Report
        uses: simple-elf/allure-report-action@v1.7
        with:
          allure_results: allure-results
          allure_history: allure-history

      - name: Deploy to GitHub Pages
        uses: peaceiris/actions-gh-pages@v3
        with:
          github_token: ${{ secrets.GITHUB_TOKEN }}
          publish_dir: allure-history
```

---

## 6. Quick Start Commands

```bash
# Run all tests
mvn test

# Run specific domain
mvn test -Ppayments

# Run with specific environment
mvn test -Dspring.profiles.active=staging

# Run with specific browser
mvn test -Dframework.browser.type=firefox

# Run in headed mode (for debugging)
mvn test -Dframework.browser.headless=false

# Run with increased parallelism
mvn test -Dthread.count=10

# Generate Allure report
mvn allure:serve

# Run single test class
mvn test -Dtest=PaymentTests

# Run single test method
mvn test -Dtest=PaymentTests#testCreditCardPayment
```

---

*Document End - Directory Structure v1.0*
