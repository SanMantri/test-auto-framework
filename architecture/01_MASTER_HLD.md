# Test Automation Framework - Master High-Level Design (HLD)

## Document Information
| Attribute | Value |
|-----------|-------|
| Version | 1.0 |
| Author | Principal Architect |
| Stack | Java 17+ / SpringBoot / TestNG / Maven / Playwright / RestAssured |
| Domains | Payments, Movie Booking, Playbook, Dashboard |

---

## 1. Executive Summary

This document defines the high-level architecture for a **hybrid API-UI test automation framework** designed to validate complex business workflows across four distinct domains. The architecture prioritizes:

- **Speed**: API-driven test setup, parallel execution, single authentication
- **Reliability**: Isolated browser contexts, deterministic data, retry mechanisms
- **Maintainability**: Domain-driven organization, reusable playbooks, centralized configuration
- **Observability**: Full network/console capture, structured reporting, trace correlation

---

## 2. Architecture Principles

### 2.1 Core Tenets

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         ARCHITECTURE PRINCIPLES                              │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  1. API-FIRST TEST SETUP                                                     │
│     └── Use API to create preconditions, UI only for critical interactions  │
│                                                                              │
│  2. SINGLE AUTHENTICATION                                                    │
│     └── Login once per suite, inject cookies/storage state per test         │
│                                                                              │
│  3. DOMAIN-DRIVEN ORGANIZATION                                               │
│     └── Each business domain owns its tests, pages, APIs, and data          │
│                                                                              │
│  4. PLAYBOOK COMPOSITION                                                     │
│     └── Import and compose workflows, never duplicate                        │
│                                                                              │
│  5. PARALLEL-SAFE BY DEFAULT                                                 │
│     └── Isolated contexts, scoped data caches, no shared mutable state      │
│                                                                              │
│  6. FULL OBSERVABILITY                                                       │
│     └── Network logs, console errors, screenshots, traces on failure        │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 2.2 Hybrid Testing Model

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                                                                              │
│                          HYBRID TESTING MODEL                                │
│                                                                              │
│  ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐          │
│  │   TRADITIONAL   │    │     HYBRID      │    │   API-ONLY     │          │
│  │    UI TEST      │    │   (OUR MODEL)   │    │     TEST       │          │
│  ├─────────────────┤    ├─────────────────┤    ├─────────────────┤          │
│  │ UI: Login       │    │ API: Login      │    │ API: Login     │          │
│  │ UI: Search      │    │ API: Add to Cart│    │ API: Add Cart  │          │
│  │ UI: Add Cart    │    │ UI: Checkout    │    │ API: Checkout  │          │
│  │ UI: Checkout    │    │ UI: Pay         │    │ API: Pay       │          │
│  │ UI: Pay         │    │ API: Verify     │    │ API: Verify    │          │
│  ├─────────────────┤    ├─────────────────┤    ├─────────────────┤          │
│  │ Time: 45-60s    │    │ Time: 8-12s     │    │ Time: 2-3s     │          │
│  │ Flakiness: High │    │ Flakiness: Low  │    │ Flakiness: Min │          │
│  │ Coverage: Full  │    │ Coverage: Smart │    │ Coverage: API  │          │
│  └─────────────────┘    └─────────────────┘    └─────────────────┘          │
│                                                                              │
│                              ▲                                               │
│                              │                                               │
│                     WE USE THIS MODEL                                        │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 3. System Architecture Overview

### 3.1 High-Level Component Diagram

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                                  TEST AUTOMATION FRAMEWORK                               │
├─────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                          │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐    │
│  │                              EXECUTION LAYER                                     │    │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐            │    │
│  │  │   TestNG    │  │   Maven     │  │  Parallel   │  │  Sharding   │            │    │
│  │  │   Runner    │  │   Surefire  │  │  Executor   │  │  Controller │            │    │
│  │  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘            │    │
│  └─────────┼────────────────┼────────────────┼────────────────┼────────────────────┘    │
│            │                │                │                │                          │
│            ▼                ▼                ▼                ▼                          │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐    │
│  │                           ORCHESTRATION LAYER                                    │    │
│  │                                                                                  │    │
│  │  ┌───────────────────┐  ┌───────────────────┐  ┌───────────────────┐           │    │
│  │  │  Authentication   │  │    Context Pool   │  │   Test Lifecycle  │           │    │
│  │  │    Manager        │  │     Manager       │  │     Manager       │           │    │
│  │  │                   │  │                   │  │                   │           │    │
│  │  │ • Login once      │  │ • Browser pool    │  │ • @BeforeSuite    │           │    │
│  │  │ • Store state     │  │ • Context reuse   │  │ • @BeforeMethod   │           │    │
│  │  │ • Inject cookies  │  │ • Isolation       │  │ • @AfterMethod    │           │    │
│  │  └───────────────────┘  └───────────────────┘  └───────────────────┘           │    │
│  │                                                                                  │    │
│  └──────────────────────────────────┬───────────────────────────────────────────────┘    │
│                                     │                                                    │
│                                     ▼                                                    │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐    │
│  │                              DOMAIN LAYER                                        │    │
│  │                                                                                  │    │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐        │    │
│  │  │   PAYMENTS   │  │   BOOKING    │  │   PLAYBOOK   │  │  DASHBOARD   │        │    │
│  │  │              │  │              │  │              │  │              │        │    │
│  │  │ • Cart       │  │ • Search     │  │ • Workflows  │  │ • Charts     │        │    │
│  │  │ • Checkout   │  │ • Seats      │  │ • Actions    │  │ • Filters    │        │    │
│  │  │ • Payment    │  │ • Booking    │  │ • Triggers   │  │ • Export     │        │    │
│  │  │ • Refund     │  │ • Cancel     │  │ • Schedule   │  │ • Drill-down │        │    │
│  │  └──────────────┘  └──────────────┘  └──────────────┘  └──────────────┘        │    │
│  │                                                                                  │    │
│  └──────────────────────────────────┬───────────────────────────────────────────────┘    │
│                                     │                                                    │
│                                     ▼                                                    │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐    │
│  │                           INFRASTRUCTURE LAYER                                   │    │
│  │                                                                                  │    │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐            │    │
│  │  │  Playwright │  │ RestAssured │  │    Data     │  │  Reporting  │            │    │
│  │  │   Driver    │  │   Client    │  │   Manager   │  │   Engine    │            │    │
│  │  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘            │    │
│  │                                                                                  │    │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐            │    │
│  │  │   Config    │  │    Log      │  │   Network   │  │   Visual    │            │    │
│  │  │   Manager   │  │   Capture   │  │   Capture   │  │   Testing   │            │    │
│  │  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘            │    │
│  │                                                                                  │    │
│  └─────────────────────────────────────────────────────────────────────────────────┘    │
│                                                                                          │
└─────────────────────────────────────────────────────────────────────────────────────────┘
```

### 3.2 Layer Responsibilities

| Layer | Responsibility | Key Components |
|-------|---------------|----------------|
| **Execution** | Test discovery, parallel distribution, sharding | TestNG, Maven Surefire, Thread Pools |
| **Orchestration** | Lifecycle management, authentication, context pooling | Auth Manager, Context Pool, Hooks |
| **Domain** | Business logic, feature-specific tests, page objects | Per-domain modules |
| **Infrastructure** | Browser automation, API clients, utilities | Playwright, RestAssured, Reporters |

---

## 4. Authentication Architecture

### 4.1 Single Sign-On Flow

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                            AUTHENTICATION FLOW                                           │
├─────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                          │
│  PHASE 1: SUITE INITIALIZATION (Once per test run)                                      │
│  ─────────────────────────────────────────────────                                      │
│                                                                                          │
│  ┌─────────────┐      ┌─────────────┐      ┌─────────────┐      ┌─────────────┐        │
│  │  @Before    │      │   API       │      │   Store     │      │   Create    │        │
│  │  Suite      │─────▶│   Login     │─────▶│   Cookies   │─────▶│   Storage   │        │
│  │             │      │   (2-3s)    │      │   + Tokens  │      │   State     │        │
│  └─────────────┘      └─────────────┘      └─────────────┘      └─────────────┘        │
│                                                                        │                │
│                                                                        ▼                │
│                                                          ┌─────────────────────────┐   │
│                                                          │   auth-state.json       │   │
│                                                          │   ─────────────────     │   │
│                                                          │   • cookies[]           │   │
│                                                          │   • localStorage{}      │   │
│                                                          │   • sessionStorage{}    │   │
│                                                          └─────────────────────────┘   │
│                                                                        │                │
│  PHASE 2: TEST EXECUTION (Per test)                                    │                │
│  ──────────────────────────────────                                    │                │
│                                                                        ▼                │
│  ┌─────────────┐      ┌─────────────┐      ┌─────────────┐      ┌─────────────┐        │
│  │  @Before    │      │   Create    │      │   Inject    │      │   Navigate  │        │
│  │  Method     │─────▶│   Context   │─────▶│   Storage   │─────▶│   Direct    │        │
│  │             │      │   (50ms)    │      │   State     │      │   to Page   │        │
│  └─────────────┘      └─────────────┘      └─────────────┘      └─────────────┘        │
│                                                                                          │
│                                                                                          │
│  RESULT: Every test starts in authenticated state without UI login                      │
│  ────────────────────────────────────────────────────────────────                       │
│                                                                                          │
│  Traditional: 100 tests × 15s login = 25 minutes                                        │
│  Our Model:   1 API login + 100 tests × 0.05s inject = 5 seconds                        │
│                                                                                          │
└─────────────────────────────────────────────────────────────────────────────────────────┘
```

### 4.2 Multi-User Authentication

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                         MULTI-USER AUTHENTICATION                                        │
├─────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                          │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐    │
│  │                          AuthenticationManager                                   │    │
│  │                                                                                  │    │
│  │  ┌─────────────────────────────────────────────────────────────────────────┐   │    │
│  │  │                     Storage State Cache                                  │   │    │
│  │  │                                                                          │   │    │
│  │  │   ┌──────────────┐  ┌──────────────┐  ┌──────────────┐                 │   │    │
│  │  │   │  ADMIN_USER  │  │ NORMAL_USER  │  │  GUEST_USER  │                 │   │    │
│  │  │   │              │  │              │  │              │                 │   │    │
│  │  │   │ state.json   │  │ state.json   │  │ state.json   │                 │   │    │
│  │  │   │ permissions: │  │ permissions: │  │ permissions: │                 │   │    │
│  │  │   │  [ALL]       │  │  [READ,WRITE]│  │  [READ]      │                 │   │    │
│  │  │   └──────────────┘  └──────────────┘  └──────────────┘                 │   │    │
│  │  │                                                                          │   │    │
│  │  └─────────────────────────────────────────────────────────────────────────┘   │    │
│  │                                                                                  │    │
│  │  getAuthState(UserRole role) → StorageState                                     │    │
│  │  createContextWithAuth(UserRole role) → BrowserContext                          │    │
│  │                                                                                  │    │
│  └─────────────────────────────────────────────────────────────────────────────────┘    │
│                                                                                          │
│  Usage in Tests:                                                                         │
│  ───────────────                                                                         │
│                                                                                          │
│  @Test(groups = "admin")                                                                │
│  public void adminCanDeleteUser() {                                                     │
│      BrowserContext ctx = authManager.createContextWithAuth(UserRole.ADMIN);            │
│      Page page = ctx.newPage();                                                         │
│      page.navigate("/admin/users");  // Already logged in as admin                      │
│  }                                                                                       │
│                                                                                          │
└─────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 5. Data Management Architecture

### 5.1 Three-Tier Data Cache

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                              DATA MANAGEMENT TIERS                                       │
├─────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                          │
│  TIER 1: GlobalDataCache (Suite-Level)                                                  │
│  ─────────────────────────────────────                                                  │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐    │
│  │  ConcurrentHashMap<String, Object> - Thread-safe, shared across all tests       │    │
│  │                                                                                  │    │
│  │  • Configuration values                                                          │    │
│  │  • Master data (countries, currencies)                                           │    │
│  │  • Cached API responses                                                          │    │
│  │  • Authentication tokens                                                         │    │
│  │                                                                                  │    │
│  │  Lifecycle: @BeforeSuite → @AfterSuite                                          │    │
│  └─────────────────────────────────────────────────────────────────────────────────┘    │
│                                                                                          │
│  TIER 2: TestDataCache (Test-Level)                                                     │
│  ──────────────────────────────────                                                     │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐    │
│  │  ThreadLocal<HashMap<String, Object>> - Isolated per test thread               │    │
│  │                                                                                  │    │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐            │    │
│  │  │  Thread 1   │  │  Thread 2   │  │  Thread 3   │  │  Thread N   │            │    │
│  │  │             │  │             │  │             │  │             │            │    │
│  │  │ orderId:123 │  │ orderId:456 │  │ orderId:789 │  │ orderId:... │            │    │
│  │  │ userId:A    │  │ userId:B    │  │ userId:C    │  │ userId:...  │            │    │
│  │  │ cartId:X    │  │ cartId:Y    │  │ cartId:Z    │  │ cartId:...  │            │    │
│  │  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘            │    │
│  │                                                                                  │    │
│  │  Lifecycle: @BeforeMethod → @AfterMethod                                        │    │
│  └─────────────────────────────────────────────────────────────────────────────────┘    │
│                                                                                          │
│  TIER 3: StepDataCache (Step-Level)                                                     │
│  ──────────────────────────────────                                                     │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐    │
│  │  Transient data within a single test method                                     │    │
│  │                                                                                  │    │
│  │  • Intermediate calculation results                                              │    │
│  │  • Temporary element references                                                  │    │
│  │  • Playbook step outputs                                                         │    │
│  │                                                                                  │    │
│  │  Lifecycle: Within method scope                                                  │    │
│  └─────────────────────────────────────────────────────────────────────────────────┘    │
│                                                                                          │
└─────────────────────────────────────────────────────────────────────────────────────────┘
```

### 5.2 Data Flow Between Caches

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                              DATA FLOW PATTERN                                           │
├─────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                          │
│   @BeforeSuite                                                                           │
│   ────────────                                                                           │
│        │                                                                                 │
│        ▼                                                                                 │
│   ┌─────────────────────────────────────────────────────────────────┐                   │
│   │  GlobalDataCache.put("baseUrl", config.getBaseUrl())            │                   │
│   │  GlobalDataCache.put("authState", authenticator.login())        │                   │
│   │  GlobalDataCache.put("masterData", api.getMasterData())         │                   │
│   └─────────────────────────────────────────────────────────────────┘                   │
│        │                                                                                 │
│        │  ┌────────────────────────────────────────────────────────┐                    │
│        │  │ Parallel Test Execution                                │                    │
│        │  │                                                        │                    │
│        ▼  ▼                                                        │                    │
│   ┌──────────────────────┐  ┌──────────────────────┐               │                    │
│   │  Thread 1: Test A    │  │  Thread 2: Test B    │               │                    │
│   │                      │  │                      │               │                    │
│   │  @BeforeMethod       │  │  @BeforeMethod       │               │                    │
│   │  ───────────────     │  │  ───────────────     │               │                    │
│   │  TestDataCache       │  │  TestDataCache       │               │                    │
│   │    .clear()          │  │    .clear()          │               │                    │
│   │                      │  │                      │               │                    │
│   │  // Read from Global │  │  // Read from Global │               │                    │
│   │  url = Global.get()  │  │  url = Global.get()  │               │                    │
│   │                      │  │                      │               │                    │
│   │  // Write to Local   │  │  // Write to Local   │               │                    │
│   │  TestData.put(       │  │  TestData.put(       │               │                    │
│   │    "orderId",        │  │    "orderId",        │               │                    │
│   │    api.createOrder() │  │    api.createOrder() │               │                    │
│   │  )                   │  │  )                   │               │                    │
│   │                      │  │                      │               │                    │
│   │  @Test               │  │  @Test               │               │                    │
│   │  ─────               │  │  ─────               │               │                    │
│   │  orderId = TestData  │  │  orderId = TestData  │               │                    │
│   │    .get("orderId")   │  │    .get("orderId")   │               │                    │
│   │  // Each thread has  │  │  // Each thread has  │               │                    │
│   │  // its own orderId  │  │  // its own orderId  │               │                    │
│   │                      │  │                      │               │                    │
│   └──────────────────────┘  └──────────────────────┘               │                    │
│                                                                    │                    │
│   └────────────────────────────────────────────────────────────────┘                    │
│                                                                                          │
└─────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 6. Parallel Execution Architecture

### 6.1 Four Levels of Parallelism

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                           PARALLELISM LEVELS                                             │
├─────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                          │
│  LEVEL 1: CI/CD SHARDING (Cross-Machine)                                                │
│  ────────────────────────────────────────                                               │
│                                                                                          │
│  ┌────────────┐  ┌────────────┐  ┌────────────┐  ┌────────────┐                        │
│  │  Runner 1  │  │  Runner 2  │  │  Runner 3  │  │  Runner 4  │                        │
│  │            │  │            │  │            │  │            │                        │
│  │ Tests 1-25 │  │ Tests 26-50│  │ Tests 51-75│  │Tests 76-100│                        │
│  └────────────┘  └────────────┘  └────────────┘  └────────────┘                        │
│                                                                                          │
│  GitHub Actions Matrix:                                                                  │
│  strategy:                                                                               │
│    matrix:                                                                               │
│      shard: [1, 2, 3, 4]                                                                │
│                                                                                          │
│  ─────────────────────────────────────────────────────────────────────────────────────  │
│                                                                                          │
│  LEVEL 2: SUITE PARALLELISM (TestNG Suites)                                             │
│  ──────────────────────────────────────────                                             │
│                                                                                          │
│  ┌────────────────────────────────────────────────────────────────────────────────┐     │
│  │  <suite name="Regression" parallel="suites" thread-count="4">                  │     │
│  │      <suite-files>                                                             │     │
│  │          <suite-file path="payments-suite.xml"/>                               │     │
│  │          <suite-file path="booking-suite.xml"/>                                │     │
│  │          <suite-file path="playbook-suite.xml"/>                               │     │
│  │          <suite-file path="dashboard-suite.xml"/>                              │     │
│  │      </suite-files>                                                            │     │
│  │  </suite>                                                                      │     │
│  └────────────────────────────────────────────────────────────────────────────────┘     │
│                                                                                          │
│  ─────────────────────────────────────────────────────────────────────────────────────  │
│                                                                                          │
│  LEVEL 3: TEST PARALLELISM (Within Suite)                                               │
│  ────────────────────────────────────────                                               │
│                                                                                          │
│  ┌────────────────────────────────────────────────────────────────────────────────┐     │
│  │  <suite name="Payments" parallel="methods" thread-count="5">                   │     │
│  │      <test name="PaymentTests">                                                │     │
│  │          <classes>                                                             │     │
│  │              <class name="CheckoutTest"/>   ← 5 methods run in parallel       │     │
│  │              <class name="RefundTest"/>                                        │     │
│  │          </classes>                                                            │     │
│  │      </test>                                                                   │     │
│  │  </suite>                                                                      │     │
│  └────────────────────────────────────────────────────────────────────────────────┘     │
│                                                                                          │
│  ─────────────────────────────────────────────────────────────────────────────────────  │
│                                                                                          │
│  LEVEL 4: BROWSER CONTEXT PARALLELISM (Playwright)                                      │
│  ─────────────────────────────────────────────────                                      │
│                                                                                          │
│  ┌────────────────────────────────────────────────────────────────────────────────┐     │
│  │  Single Browser Instance                                                       │     │
│  │  ┌─────────────────────────────────────────────────────────────────────────┐  │     │
│  │  │  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐       │  │     │
│  │  │  │Context 1│  │Context 2│  │Context 3│  │Context 4│  │Context 5│       │  │     │
│  │  │  │         │  │         │  │         │  │         │  │         │       │  │     │
│  │  │  │ Test A  │  │ Test B  │  │ Test C  │  │ Test D  │  │ Test E  │       │  │     │
│  │  │  │ Page 1  │  │ Page 1  │  │ Page 1  │  │ Page 1  │  │ Page 1  │       │  │     │
│  │  │  │ State:X │  │ State:Y │  │ State:Z │  │ State:W │  │ State:V │       │  │     │
│  │  │  └─────────┘  └─────────┘  └─────────┘  └─────────┘  └─────────┘       │  │     │
│  │  │                                                                         │  │     │
│  │  │  Each context is FULLY ISOLATED:                                        │  │     │
│  │  │  • Own cookies                                                          │  │     │
│  │  │  • Own localStorage                                                     │  │     │
│  │  │  • Own session                                                          │  │     │
│  │  │  • Own network cache                                                    │  │     │
│  │  └─────────────────────────────────────────────────────────────────────────┘  │     │
│  └────────────────────────────────────────────────────────────────────────────────┘     │
│                                                                                          │
└─────────────────────────────────────────────────────────────────────────────────────────┘
```

### 6.2 Parallelism Configuration Matrix

| Level | Scope | Config Location | Recommended Value |
|-------|-------|-----------------|-------------------|
| Sharding | CI/CD | GitHub Actions matrix | 4-8 runners |
| Suite | TestNG | testng.xml parallel="suites" | 4 (one per domain) |
| Method | TestNG | testng.xml thread-count | 5-10 per suite |
| Context | Playwright | BrowserContextPool | 1 browser, N contexts |

---

## 7. Domain Integration Points

### 7.1 Cross-Domain Test Flow

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                         CROSS-DOMAIN TEST EXAMPLE                                        │
│                   "Complete Movie Ticket Purchase with Payment"                          │
├─────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                          │
│  ┌──────────────────┐         ┌──────────────────┐         ┌──────────────────┐         │
│  │  BOOKING DOMAIN  │         │  PAYMENT DOMAIN  │         │ DASHBOARD DOMAIN │         │
│  │                  │         │                  │         │                  │         │
│  │  1. Search Movie │         │  4. Select       │         │  7. Verify       │         │
│  │     (API)        │────────▶│     Payment      │────────▶│     Booking in   │         │
│  │                  │         │     Method (UI)  │         │     Dashboard    │         │
│  │  2. Select Seats │         │                  │         │     (API + UI)   │         │
│  │     (UI)         │         │  5. Enter Card   │         │                  │         │
│  │                  │         │     Details (UI) │         │  8. Verify       │         │
│  │  3. Confirm      │         │                  │         │     Revenue      │         │
│  │     Selection    │         │  6. Complete     │         │     Chart        │         │
│  │     (UI)         │         │     Payment (UI) │         │     (Visual)     │         │
│  │                  │         │                  │         │                  │         │
│  └──────────────────┘         └──────────────────┘         └──────────────────┘         │
│                                                                                          │
│  Test Implementation:                                                                    │
│  ────────────────────                                                                    │
│                                                                                          │
│  @Test                                                                                   │
│  public void completeMovieTicketPurchaseE2E() {                                         │
│      // BOOKING: API Setup                                                              │
│      String showId = bookingApi.getAvailableShow("Inception", "Mumbai");                │
│      String[] seats = bookingApi.holdSeats(showId, 2);                                  │
│                                                                                          │
│      // BOOKING: UI Confirmation                                                         │
│      bookingPage.navigate(showId);                                                       │
│      bookingPage.confirmSeatSelection(seats);                                            │
│                                                                                          │
│      // PAYMENT: UI Flow                                                                 │
│      paymentPage.selectPaymentMethod("CREDIT_CARD");                                    │
│      paymentPage.enterCardDetails(testCard);                                             │
│      String orderId = paymentPage.completePayment();                                     │
│                                                                                          │
│      // DASHBOARD: Verification                                                          │
│      assertThat(dashboardApi.getOrder(orderId).status()).isEqualTo("CONFIRMED");        │
│      dashboardPage.navigateToBookings();                                                 │
│      assertThat(dashboardPage.isBookingVisible(orderId)).isTrue();                      │
│  }                                                                                       │
│                                                                                          │
└─────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 8. Reporting & Observability

### 8.1 Multi-Layer Reporting

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                           REPORTING ARCHITECTURE                                         │
├─────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                          │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐    │
│  │                              TEST EXECUTION                                      │    │
│  │                                                                                  │    │
│  │   @Test → Actions → Assertions → Pass/Fail                                      │    │
│  │      │         │           │          │                                          │    │
│  └──────┼─────────┼───────────┼──────────┼──────────────────────────────────────────┘    │
│         │         │           │          │                                               │
│         ▼         ▼           ▼          ▼                                               │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐    │
│  │                           CAPTURE LAYER                                          │    │
│  │                                                                                  │    │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐            │    │
│  │  │  Network    │  │  Console    │  │ Screenshots │  │   Trace     │            │    │
│  │  │  Logs       │  │  Logs       │  │  (Failure)  │  │  Recording  │            │    │
│  │  │             │  │             │  │             │  │             │            │    │
│  │  │ • Requests  │  │ • Errors    │  │ • Full page │  │ • Actions   │            │    │
│  │  │ • Responses │  │ • Warnings  │  │ • Element   │  │ • Network   │            │    │
│  │  │ • Timing    │  │ • Info      │  │ • Diff      │  │ • Console   │            │    │
│  │  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘            │    │
│  │                                                                                  │    │
│  └──────────────────────────────────┬───────────────────────────────────────────────┘    │
│                                     │                                                    │
│                                     ▼                                                    │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐    │
│  │                           REPORT GENERATION                                      │    │
│  │                                                                                  │    │
│  │  ┌─────────────────────────────────────────────────────────────────────────┐   │    │
│  │  │                         Allure Report                                    │   │    │
│  │  │                                                                          │   │    │
│  │  │  • Test results with steps                                               │   │    │
│  │  │  • Attachments (screenshots, logs, traces)                               │   │    │
│  │  │  • Categories (by domain, severity)                                      │   │    │
│  │  │  • Trends over time                                                      │   │    │
│  │  │  • Environment information                                               │   │    │
│  │  └─────────────────────────────────────────────────────────────────────────┘   │    │
│  │                                                                                  │    │
│  │  ┌─────────────────────────────────────────────────────────────────────────┐   │    │
│  │  │                       Custom JSON Report                                 │   │    │
│  │  │                                                                          │   │    │
│  │  │  • Machine-readable results                                              │   │    │
│  │  │  • Integration with dashboards                                           │   │    │
│  │  │  • Metrics export                                                        │   │    │
│  │  └─────────────────────────────────────────────────────────────────────────┘   │    │
│  │                                                                                  │    │
│  └─────────────────────────────────────────────────────────────────────────────────┘    │
│                                                                                          │
└─────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 9. Technology Stack Summary

| Category | Technology | Version | Purpose |
|----------|-----------|---------|---------|
| **Language** | Java | 17+ | Core implementation |
| **Build** | Maven | 3.9+ | Dependency management, build lifecycle |
| **Framework** | SpringBoot | 3.x | DI, configuration, bean management |
| **Test Runner** | TestNG | 7.x | Test organization, parallel execution |
| **UI Automation** | Playwright | 1.40+ | Browser automation, multi-browser |
| **API Testing** | RestAssured | 5.x | REST API testing |
| **Assertions** | AssertJ | 3.x | Fluent assertions |
| **Reporting** | Allure | 2.x | Rich HTML reports |
| **Logging** | SLF4J + Logback | 1.4+ | Structured logging |
| **Visual Testing** | Applitools/Percy | Latest | AI-powered visual comparison |

---

## 10. Next Steps

This HLD provides the architectural foundation. The following documents will detail:

1. **02_PAYMENTS_LLD.md** - Detailed design for payment flow testing
2. **03_BOOKING_LLD.md** - Detailed design for movie booking testing
3. **04_PLAYBOOK_LLD.md** - Detailed design for workflow automation
4. **05_DASHBOARD_LLD.md** - Detailed design for dashboard/visualization testing
5. **06_DATA_FLOW_DIAGRAMS.md** - Complete DFDs for all flows
6. **07_DIRECTORY_STRUCTURE.md** - Project organization and code examples

---

*Document End - Master HLD v1.0*
