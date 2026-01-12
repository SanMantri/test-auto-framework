# UI Automation Framework

A hybrid API-UI test automation framework built with **Playwright**, **TestNG**, **Spring Boot**, and **RestAssured**.

## Prerequisites

- **Java 17+**
- **Maven 3.8+**
- **Playwright browsers** (installed automatically on first run)

## Quick Start

```bash
# Install dependencies and Playwright browsers
mvn clean install -DskipTests
mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install"

# Run all tests (dev environment)
mvn test

# Run with Allure report
mvn test
mvn allure:serve
```

## Running Tests

### By Environment

```bash
# Development (default)
mvn test -Pdev

# Staging
mvn test -Pstaging

# Production (smoke tests only)
mvn test -Pprod
```

### By Domain

```bash
# Payments tests
mvn test -Ppayments

# Booking tests
mvn test -Pbooking

# Playbook tests
mvn test -Pplaybook

# Dashboard tests
mvn test -Pdashboard
```

### By Test Type

```bash
# Smoke tests only
mvn test -Psmoke

# Specific test class
mvn test -Dtest=PaymentFlowTest

# Specific test method
mvn test -Dtest=PaymentFlowTest#testSuccessfulVisaPayment

# Tests matching pattern
mvn test -Dtest=*BookingFlow*
```

### Parallel Execution

```bash
# Run with 8 parallel threads
mvn test -Dthread.count=8

# Run with specific parallelism level
mvn test -Dparallel.tests=10
```

### Headed Mode (for debugging)

```bash
# Run with visible browser
mvn test -DHEADLESS=false

# Run with slow motion (500ms delay between actions)
mvn test -DHEADLESS=false -DSLOW_MO=500
```

### CI/CD Sharding

```bash
# Shard 1 of 4
mvn test -Dshard.index=1 -Dshard.total=4

# Shard 2 of 4
mvn test -Dshard.index=2 -Dshard.total=4
```

## Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `ENVIRONMENT` | Target environment (dev/staging/prod) | `dev` |
| `BASE_URL` | Application base URL | `https://dev.example.com` |
| `API_URL` | API base URL | `https://api.dev.example.com` |
| `BROWSER` | Browser type (chromium/firefox/webkit) | `chromium` |
| `HEADLESS` | Run headless | `true` |
| `SLOW_MO` | Delay between actions (ms) | `0` |
| `ADMIN_USER` | Admin username | - |
| `ADMIN_PASSWORD` | Admin password | - |
| `TEST_USER` | Test user username | - |
| `TEST_PASSWORD` | Test user password | - |

### Configuration Files

```
src/main/resources/
├── application.yml          # Base configuration
├── application-dev.yml      # Development overrides
├── application-staging.yml  # Staging overrides
└── application-prod.yml     # Production overrides
```

## Test Reports

### Allure Reports

```bash
# Generate and open Allure report
mvn allure:serve

# Generate report only (without serving)
mvn allure:report
```

Report includes:
- Test execution timeline
- Screenshots on failure
- Network request/response logs
- Console error logs
- Playwright traces (when enabled)

### Report Locations

| Artifact | Location |
|----------|----------|
| Allure results | `target/allure-results/` |
| Allure report | `target/site/allure-maven-plugin/` |
| Screenshots | `target/screenshots/` |
| Traces | `target/traces/` |

## Project Structure

```
ui-automation-framework/
├── src/main/java/com/framework/
│   ├── core/                    # Framework core
│   │   ├── base/               # BaseTest, BasePage, BaseApiClient
│   │   ├── browser/            # BrowserManager
│   │   ├── auth/               # AuthenticationManager
│   │   ├── config/             # FrameworkConfig
│   │   ├── data/               # TestDataCache, GlobalDataCache
│   │   ├── reporting/          # NetworkLogger
│   │   └── listeners/          # TestNG listeners
│   │
│   └── domains/                 # Domain implementations
│       ├── payments/           # E-commerce payments
│       │   ├── api/           # PaymentApiClient, CartApiClient
│       │   ├── models/        # Order, Cart, TestCard
│       │   └── pages/         # PaymentPage, CartPage, etc.
│       │
│       ├── booking/            # Movie ticket booking
│       │   ├── api/           # BookingApiClient, MovieApiClient
│       │   ├── models/        # Booking, Movie, Seat
│       │   ├── pages/         # SeatSelectionPage, etc.
│       │   └── playbooks/     # BookingFlowPlaybook
│       │
│       ├── playbook/           # Automation playbooks
│       │   ├── api/           # PlaybookApiClient
│       │   ├── models/        # Playbook, Execution
│       │   └── pages/         # PlaybookBuilderPage
│       │
│       └── dashboard/          # Analytics dashboards
│           ├── api/           # DashboardApiClient
│           ├── models/        # Dashboard, Widget
│           ├── pages/         # DashboardPage
│           └── visual/        # VisualTestingUtils
│
├── src/test/java/com/framework/tests/
│   ├── payments/               # Payment flow tests
│   ├── booking/                # Booking flow tests
│   ├── playbook/               # Playbook execution tests
│   └── dashboard/              # Dashboard tests
│
└── src/test/resources/
    ├── testng.xml              # TestNG suite configuration
    ├── test-data/              # JSON test data files
    └── visual-baselines/       # Visual regression baselines
```

## Key Features

### 1. API-First Test Setup
Tests use APIs to set up preconditions quickly, then validate UI behavior:

```java
// API: Create cart and add items
Cart cart = cartApi.createCart();
cartApi.addItem(cart.getId(), product);

// UI: Verify cart display
CartPage cartPage = new CartPage(page);
assertThat(cartPage.getItemCount()).isEqualTo(1);
```

### 2. Storage State Authentication
Login once, reuse across all tests:

```java
// Automatic per test - auth state is injected into browser context
// No login steps needed in individual tests
```

### 3. Parallel-Safe Test Data
Each test thread has isolated data via `TestDataCache`:

```java
testData().put("orderId", order.getId());
String orderId = testData().get("orderId");
```

### 4. Seat Locking for Race Prevention
Booking tests lock seats via API before UI selection:

```java
BookingTestData setup = bookingSetupPlaybook.setupBookingScenario(2, testData);
// Seats are locked, safe from parallel test interference
```

### 5. Visual Regression Testing
Dashboard tests include pixel-level comparison:

```java
visualTestingUtils.compareWithBaseline(page, "dashboard-kpi");
```

## Debugging

### Enable Tracing

```bash
mvn test -DTRACE_ENABLED=true
```

View trace:
```bash
mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI \
  -D exec.args="show-trace target/traces/testName.zip"
```

### Run Single Test in Headed Mode

```bash
mvn test -Dtest=PaymentFlowTest#testSuccessfulVisaPayment \
  -DHEADLESS=false -DSLOW_MO=500
```

### View Network Logs

Network logs are automatically attached to Allure reports on failure.

## Troubleshooting

### Playwright browsers not installed

```bash
mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install"
```

### Tests timeout on slow network

```bash
mvn test -Dframework.browser.default-timeout=60000
```

### Out of memory errors

```bash
mvn test -DargLine="-Xmx2g"
```

### Spring context issues

```bash
mvn clean test -DskipTests=false
```
