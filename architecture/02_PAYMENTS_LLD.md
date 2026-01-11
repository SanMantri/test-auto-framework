# Payments Domain - Low-Level Design (LLD)

## Document Information
| Attribute | Value |
|-----------|-------|
| Domain | Payments (Amazon-style e-commerce) |
| Version | 1.0 |
| Dependencies | Master HLD |

---

## 1. Domain Overview

### 1.1 What We're Testing

The Payments domain covers the complete e-commerce transaction lifecycle:

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                           PAYMENTS DOMAIN SCOPE                                          │
├─────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                          │
│  ┌───────────────────────────────────────────────────────────────────────────────────┐  │
│  │                                                                                    │  │
│  │   CART              CHECKOUT            PAYMENT             POST-PAYMENT          │  │
│  │   ────              ────────            ───────             ────────────          │  │
│  │                                                                                    │  │
│  │   • Add items       • Address           • Card entry        • Order confirmation  │  │
│  │   • Update qty      • Shipping          • UPI/Wallet        • Invoice generation  │  │
│  │   • Remove items    • Gift wrap         • Net banking       • Refund processing   │  │
│  │   • Apply coupon    • Order summary     • EMI options       • Partial refund      │  │
│  │   • Save for later  • Tax calculation   • 3DS/OTP flow      • Chargeback          │  │
│  │                                                                                    │  │
│  └───────────────────────────────────────────────────────────────────────────────────┘  │
│                                                                                          │
└─────────────────────────────────────────────────────────────────────────────────────────┘
```

### 1.2 Testing Philosophy for Payments

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                         PAYMENTS TESTING PHILOSOPHY                                      │
├─────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                          │
│  PRINCIPLE: "Setup via API, Pay via UI, Verify via API"                                │
│                                                                                          │
│  WHY?                                                                                    │
│  ────                                                                                    │
│  • Cart operations are CRUD - API is faster and more reliable                           │
│  • Payment UI is critical user touchpoint - must test actual UI                         │
│  • Order verification via API gives accurate data, not DOM scraping                     │
│                                                                                          │
│  TRADITIONAL APPROACH (Slow, Flaky)          OUR APPROACH (Fast, Reliable)              │
│  ──────────────────────────────────          ────────────────────────────               │
│                                                                                          │
│  1. UI: Login                                1. API: Get auth token (0.5s)              │
│  2. UI: Search product                       2. API: Add 3 items to cart (0.3s)         │
│  3. UI: Click Add to Cart                    3. API: Apply coupon (0.1s)                │
│  4. UI: Search another product               4. UI: Navigate to checkout (1s)           │
│  5. UI: Click Add to Cart                    5. UI: Select address (0.5s)               │
│  6. UI: Go to Cart                           6. UI: Enter card details (2s)             │
│  7. UI: Apply coupon                         7. UI: Complete payment (1s)               │
│  8. UI: Proceed to Checkout                  8. API: Verify order created (0.2s)        │
│  9. UI: Select address                                                                   │
│  10. UI: Enter card                          Total: ~5.6 seconds                        │
│  11. UI: Submit payment                                                                  │
│  12. UI: Verify order                                                                    │
│                                                                                          │
│  Total: ~45-60 seconds                                                                   │
│                                                                                          │
│  SPEEDUP: 8-10x faster                                                                  │
│  RELIABILITY: No flaky search, no stale elements                                        │
│                                                                                          │
└─────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Component Architecture

### 2.1 Payments Module Structure

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                          PAYMENTS MODULE COMPONENTS                                      │
├─────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                          │
│  payments/                                                                               │
│  ├── api/                          # API Layer                                          │
│  │   ├── CartApiClient.java        # Cart CRUD operations                               │
│  │   ├── CheckoutApiClient.java    # Checkout operations                                │
│  │   ├── OrderApiClient.java       # Order queries                                      │
│  │   └── PaymentApiClient.java     # Payment gateway interactions                       │
│  │                                                                                       │
│  ├── pages/                        # UI Layer (Page Objects)                            │
│  │   ├── CartPage.java             # Cart page interactions                             │
│  │   ├── CheckoutPage.java         # Checkout flow                                      │
│  │   ├── PaymentPage.java          # Payment entry                                      │
│  │   ├── OrderConfirmationPage.java # Confirmation page                                 │
│  │   └── components/               # Reusable UI components                             │
│  │       ├── AddressSelector.java                                                       │
│  │       ├── PaymentMethodSelector.java                                                 │
│  │       └── OrderSummaryPanel.java                                                     │
│  │                                                                                       │
│  ├── models/                       # Data Models                                        │
│  │   ├── CartItem.java                                                                  │
│  │   ├── Address.java                                                                   │
│  │   ├── PaymentMethod.java                                                             │
│  │   ├── Order.java                                                                     │
│  │   └── builders/                 # Test Data Builders                                 │
│  │       ├── CartBuilder.java                                                           │
│  │       └── OrderBuilder.java                                                          │
│  │                                                                                       │
│  ├── playbooks/                    # Reusable Workflows                                 │
│  │   ├── CartSetupPlaybook.java    # API: Setup cart with items                         │
│  │   ├── CheckoutPlaybook.java     # UI: Complete checkout flow                         │
│  │   └── RefundPlaybook.java       # API+UI: Process refund                             │
│  │                                                                                       │
│  ├── tests/                        # Test Classes                                       │
│  │   ├── CartTests.java                                                                 │
│  │   ├── CheckoutTests.java                                                             │
│  │   ├── PaymentTests.java                                                              │
│  │   ├── RefundTests.java                                                               │
│  │   └── E2EPaymentJourneyTests.java                                                    │
│  │                                                                                       │
│  └── data/                         # Test Data                                          │
│      ├── test-cards.json           # Test card numbers                                  │
│      ├── addresses.json            # Test addresses                                     │
│      └── products.json             # Product catalog for tests                          │
│                                                                                          │
└─────────────────────────────────────────────────────────────────────────────────────────┘
```

### 2.2 Class Diagram

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                              CLASS RELATIONSHIPS                                         │
├─────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                          │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐    │
│  │                              BaseTest                                            │    │
│  │  ─────────────────────────────────────────────────────────────────────────────  │    │
│  │  # page: Page                                                                    │    │
│  │  # context: BrowserContext                                                       │    │
│  │  # testDataCache: TestDataCache                                                  │    │
│  │  ─────────────────────────────────────────────────────────────────────────────  │    │
│  │  + setUp(): void                                                                 │    │
│  │  + tearDown(): void                                                              │    │
│  │  # getApi<T>(Class<T>): T                                                        │    │
│  │  # getPage<T>(Class<T>): T                                                       │    │
│  └─────────────────────────────────────────────────────────────────────────────────┘    │
│                                          △                                               │
│                                          │                                               │
│                                          │ extends                                       │
│                                          │                                               │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐    │
│  │                           PaymentTests                                           │    │
│  │  ─────────────────────────────────────────────────────────────────────────────  │    │
│  │  - cartApi: CartApiClient                                                        │    │
│  │  - checkoutPage: CheckoutPage                                                    │    │
│  │  - paymentPage: PaymentPage                                                      │    │
│  │  ─────────────────────────────────────────────────────────────────────────────  │    │
│  │  + testCreditCardPayment(): void                                                 │    │
│  │  + testUPIPayment(): void                                                        │    │
│  │  + testNetBanking(): void                                                        │    │
│  │  + testEMIPayment(): void                                                        │    │
│  │  + testPaymentFailureRecovery(): void                                            │    │
│  └─────────────────────────────────────────────────────────────────────────────────┘    │
│                                                                                          │
│                    uses                              uses                                │
│         ┌───────────────────────┐         ┌───────────────────────┐                     │
│         ▼                       ▼         ▼                       ▼                     │
│  ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────────────────────┐     │
│  │  CartApiClient  │    │  CheckoutPage   │    │      CartSetupPlaybook          │     │
│  │  ─────────────  │    │  ─────────────  │    │  ───────────────────────────    │     │
│  │                 │    │  - page: Page   │    │  - cartApi: CartApiClient       │     │
│  │ +addItem()      │    │                 │    │                                 │     │
│  │ +removeItem()   │    │ +selectAddress()│    │  + setupCartWithItems(          │     │
│  │ +applyCoupon()  │    │ +selectShipping()    │      List<ProductId>,           │     │
│  │ +getCart()      │    │ +proceedToPay() │    │      String couponCode          │     │
│  │                 │    │                 │    │    ): CartSummary               │     │
│  └─────────────────┘    └─────────────────┘    └─────────────────────────────────┘     │
│                                                                                          │
└─────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Test Case Design

### 3.1 Test Categories

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                              TEST CATEGORIES                                             │
├─────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                          │
│  CATEGORY 1: CART OPERATIONS (API-Only Tests)                                           │
│  ─────────────────────────────────────────────                                          │
│  These are pure API tests - no browser needed                                           │
│                                                                                          │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐   │
│  │  @Test(groups = {"cart", "api"})                                                 │   │
│  │  public void addItemToCart() {                                                   │   │
│  │      // Arrange                                                                  │   │
│  │      String productId = "PROD-123";                                              │   │
│  │      int quantity = 2;                                                           │   │
│  │                                                                                  │   │
│  │      // Act                                                                      │   │
│  │      CartResponse cart = cartApi.addItem(productId, quantity);                   │   │
│  │                                                                                  │   │
│  │      // Assert                                                                   │   │
│  │      assertThat(cart.getItems()).hasSize(1);                                     │   │
│  │      assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(2);              │   │
│  │  }                                                                               │   │
│  └─────────────────────────────────────────────────────────────────────────────────┘   │
│                                                                                          │
│  CATEGORY 2: CHECKOUT UI (Hybrid Tests)                                                 │
│  ──────────────────────────────────────                                                 │
│  API setup + UI interaction + API verification                                          │
│                                                                                          │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐   │
│  │  @Test(groups = {"checkout", "ui", "hybrid"})                                    │   │
│  │  public void completeCheckoutWithSavedAddress() {                                │   │
│  │      // ARRANGE (API) ─────────────────────────────────────────────────────────  │   │
│  │      CartSummary cart = cartPlaybook.setupCartWithItems(                         │   │
│  │          List.of("PROD-123", "PROD-456"),                                        │   │
│  │          "SAVE10"  // coupon code                                                │   │
│  │      );                                                                          │   │
│  │      testDataCache.put("expectedTotal", cart.getTotal());                        │   │
│  │                                                                                  │   │
│  │      // ACT (UI) ─────────────────────────────────────────────────────────────   │   │
│  │      checkoutPage.navigate();                                                    │   │
│  │      checkoutPage.selectSavedAddress("Home");                                    │   │
│  │      checkoutPage.selectShippingMethod("EXPRESS");                               │   │
│  │                                                                                  │   │
│  │      // UI ASSERTION ─────────────────────────────────────────────────────────   │   │
│  │      assertThat(checkoutPage.getDisplayedTotal())                                │   │
│  │          .isEqualTo(testDataCache.get("expectedTotal"));                         │   │
│  │                                                                                  │   │
│  │      // CONTINUE TO PAYMENT                                                      │   │
│  │      checkoutPage.proceedToPayment();                                            │   │
│  │  }                                                                               │   │
│  └─────────────────────────────────────────────────────────────────────────────────┘   │
│                                                                                          │
│  CATEGORY 3: PAYMENT FLOW (UI-Critical Tests)                                           │
│  ────────────────────────────────────────────                                           │
│  Payment entry MUST be tested via UI - this is user-critical                            │
│                                                                                          │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐   │
│  │  @Test(groups = {"payment", "ui", "critical"})                                   │   │
│  │  public void creditCardPaymentWith3DS() {                                        │   │
│  │      // ARRANGE (API)                                                            │   │
│  │      cartPlaybook.setupCartWithItems(List.of("PROD-123"));                       │   │
│  │      checkoutPlaybook.completeCheckout();  // Reusable UI playbook               │   │
│  │                                                                                  │   │
│  │      // ACT (UI) - Critical Payment Flow                                         │   │
│  │      paymentPage.selectPaymentMethod(PaymentMethod.CREDIT_CARD);                 │   │
│  │      paymentPage.enterCardNumber(TestCards.VISA_3DS);                            │   │
│  │      paymentPage.enterExpiry("12/28");                                           │   │
│  │      paymentPage.enterCVV("123");                                                │   │
│  │      paymentPage.clickPay();                                                     │   │
│  │                                                                                  │   │
│  │      // Handle 3DS popup                                                         │   │
│  │      paymentPage.handle3DSAuthentication("123456");                              │   │
│  │                                                                                  │   │
│  │      // ASSERT                                                                   │   │
│  │      OrderConfirmationPage confirmPage = new OrderConfirmationPage(page);        │   │
│  │      assertThat(confirmPage.isDisplayed()).isTrue();                             │   │
│  │      String orderId = confirmPage.getOrderId();                                  │   │
│  │                                                                                  │   │
│  │      // VERIFY (API) - Ensure order is in database                               │   │
│  │      Order order = orderApi.getOrder(orderId);                                   │   │
│  │      assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);             │   │
│  │      assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.CAPTURED);     │   │
│  │  }                                                                               │   │
│  └─────────────────────────────────────────────────────────────────────────────────┘   │
│                                                                                          │
│  CATEGORY 4: E2E JOURNEY (Full Flow Tests)                                              │
│  ─────────────────────────────────────────                                              │
│  Complete user journey from product to order                                            │
│                                                                                          │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐   │
│  │  @Test(groups = {"e2e", "journey", "smoke"})                                     │   │
│  │  public void guestUserCompletePurchaseJourney() {                                │   │
│  │      // This test uses MINIMAL API setup - to test the real journey             │   │
│  │      // But we still skip search and product browsing                            │   │
│  │                                                                                  │   │
│  │      // Direct add to cart (API)                                                 │   │
│  │      String cartId = cartApi.createGuestCart();                                  │   │
│  │      cartApi.addItem(cartId, "BESTSELLER-001", 1);                               │   │
│  │                                                                                  │   │
│  │      // UI Journey starts here                                                   │   │
│  │      cartPage.navigate(cartId);                                                  │   │
│  │      cartPage.proceedToCheckout();                                               │   │
│  │                                                                                  │   │
│  │      // Guest checkout - enters new address (UI)                                 │   │
│  │      checkoutPage.enterNewAddress(TestAddresses.MUMBAI_HOME);                    │   │
│  │      checkoutPage.enterContactDetails("guest@test.com", "9876543210");           │   │
│  │      checkoutPage.proceedToPayment();                                            │   │
│  │                                                                                  │   │
│  │      // Payment (UI)                                                             │   │
│  │      paymentPage.payWithCard(TestCards.VISA_SUCCESS);                            │   │
│  │                                                                                  │   │
│  │      // Verify complete journey                                                  │   │
│  │      assertThat(confirmationPage.getOrderStatus()).isEqualTo("Confirmed");       │   │
│  │      assertThat(confirmationPage.getEstimatedDelivery()).isNotNull();            │   │
│  │  }                                                                               │   │
│  └─────────────────────────────────────────────────────────────────────────────────┘   │
│                                                                                          │
└─────────────────────────────────────────────────────────────────────────────────────────┘
```

### 3.2 Test Data Strategy

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                            TEST DATA STRATEGY                                            │
├─────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                          │
│  LAYER 1: STATIC TEST DATA (JSON Files)                                                 │
│  ───────────────────────────────────────                                                │
│                                                                                          │
│  test-cards.json:                                                                        │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐   │
│  │  {                                                                               │   │
│  │    "VISA_SUCCESS": {                                                             │   │
│  │      "number": "4111111111111111",                                               │   │
│  │      "expiry": "12/28",                                                          │   │
│  │      "cvv": "123",                                                               │   │
│  │      "expectedResult": "SUCCESS"                                                 │   │
│  │    },                                                                            │   │
│  │    "VISA_DECLINED": {                                                            │   │
│  │      "number": "4000000000000002",                                               │   │
│  │      "expiry": "12/28",                                                          │   │
│  │      "cvv": "123",                                                               │   │
│  │      "expectedResult": "DECLINED"                                                │   │
│  │    },                                                                            │   │
│  │    "VISA_3DS": {                                                                 │   │
│  │      "number": "4000000000003220",                                               │   │
│  │      "expiry": "12/28",                                                          │   │
│  │      "cvv": "123",                                                               │   │
│  │      "expectedResult": "REQUIRES_3DS",                                           │   │
│  │      "otpCode": "123456"                                                         │   │
│  │    }                                                                             │   │
│  │  }                                                                               │   │
│  └─────────────────────────────────────────────────────────────────────────────────┘   │
│                                                                                          │
│  LAYER 2: DYNAMIC DATA (API-Created)                                                    │
│  ────────────────────────────────────                                                   │
│                                                                                          │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐   │
│  │  @BeforeMethod                                                                   │   │
│  │  public void setupTestData() {                                                   │   │
│  │      // Create unique cart for this test                                         │   │
│  │      String cartId = cartApi.createCart();                                       │   │
│  │      testDataCache.put("cartId", cartId);                                        │   │
│  │                                                                                  │   │
│  │      // Create unique order reference                                            │   │
│  │      String orderRef = "TEST-" + UUID.randomUUID().toString().substring(0, 8);   │   │
│  │      testDataCache.put("orderRef", orderRef);                                    │   │
│  │  }                                                                               │   │
│  │                                                                                  │   │
│  │  @AfterMethod                                                                    │   │
│  │  public void cleanupTestData() {                                                 │   │
│  │      // Clean up any orders created during test                                  │   │
│  │      String orderId = testDataCache.get("orderId");                              │   │
│  │      if (orderId != null) {                                                      │   │
│  │          orderApi.cancelOrder(orderId);  // Or mark as test data                 │   │
│  │      }                                                                           │   │
│  │  }                                                                               │   │
│  └─────────────────────────────────────────────────────────────────────────────────┘   │
│                                                                                          │
│  LAYER 3: BUILDER PATTERN (Fluent Data Creation)                                        │
│  ───────────────────────────────────────────────                                        │
│                                                                                          │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐   │
│  │  public class CartBuilder {                                                      │   │
│  │      private List<CartItem> items = new ArrayList<>();                           │   │
│  │      private String couponCode;                                                  │   │
│  │      private String userId;                                                      │   │
│  │                                                                                  │   │
│  │      public static CartBuilder aCart() {                                         │   │
│  │          return new CartBuilder();                                               │   │
│  │      }                                                                           │   │
│  │                                                                                  │   │
│  │      public CartBuilder withItem(String productId, int qty) {                    │   │
│  │          items.add(new CartItem(productId, qty));                                │   │
│  │          return this;                                                            │   │
│  │      }                                                                           │   │
│  │                                                                                  │   │
│  │      public CartBuilder withCoupon(String code) {                                │   │
│  │          this.couponCode = code;                                                 │   │
│  │          return this;                                                            │   │
│  │      }                                                                           │   │
│  │                                                                                  │   │
│  │      public CartBuilder forUser(String userId) {                                 │   │
│  │          this.userId = userId;                                                   │   │
│  │          return this;                                                            │   │
│  │      }                                                                           │   │
│  │                                                                                  │   │
│  │      public CartSummary build(CartApiClient api) {                               │   │
│  │          String cartId = api.createCart(userId);                                 │   │
│  │          for (CartItem item : items) {                                           │   │
│  │              api.addItem(cartId, item.getProductId(), item.getQuantity());       │   │
│  │          }                                                                       │   │
│  │          if (couponCode != null) {                                               │   │
│  │              api.applyCoupon(cartId, couponCode);                                │   │
│  │          }                                                                       │   │
│  │          return api.getCartSummary(cartId);                                      │   │
│  │      }                                                                           │   │
│  │  }                                                                               │   │
│  │                                                                                  │   │
│  │  // Usage:                                                                       │   │
│  │  CartSummary cart = CartBuilder.aCart()                                          │   │
│  │      .withItem("LAPTOP-001", 1)                                                  │   │
│  │      .withItem("MOUSE-002", 2)                                                   │   │
│  │      .withCoupon("SAVE20")                                                       │   │
│  │      .build(cartApi);                                                            │   │
│  └─────────────────────────────────────────────────────────────────────────────────┘   │
│                                                                                          │
└─────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 4. Page Objects Design

### 4.1 PaymentPage Implementation

```java
/**
 * PaymentPage - Handles all payment-related UI interactions
 *
 * Design Principles:
 * 1. Each method does ONE thing
 * 2. Methods return PageObject for fluent chaining
 * 3. Waits are encapsulated - caller doesn't worry about timing
 * 4. Locators are private - expose behavior, not implementation
 */
public class PaymentPage extends BasePage {

    // ═══════════════════════════════════════════════════════════════════════════
    // LOCATORS - Private, semantic names
    // ═══════════════════════════════════════════════════════════════════════════

    private static final String PAYMENT_METHOD_RADIO = "[data-testid='payment-method-%s']";
    private static final String CARD_NUMBER_INPUT = "#card-number";
    private static final String CARD_EXPIRY_INPUT = "#card-expiry";
    private static final String CARD_CVV_INPUT = "#card-cvv";
    private static final String PAY_NOW_BUTTON = "[data-testid='pay-now-btn']";
    private static final String PAYMENT_PROCESSING_LOADER = ".payment-processing";
    private static final String PAYMENT_ERROR_MESSAGE = "[data-testid='payment-error']";
    private static final String THREE_DS_IFRAME = "iframe[name='three-ds-challenge']";
    private static final String THREE_DS_OTP_INPUT = "#otp-input";
    private static final String THREE_DS_SUBMIT = "#submit-otp";

    // UPI specific
    private static final String UPI_ID_INPUT = "#upi-id";
    private static final String UPI_VERIFY_BUTTON = "[data-testid='verify-upi']";
    private static final String UPI_VERIFIED_BADGE = "[data-testid='upi-verified']";

    // ═══════════════════════════════════════════════════════════════════════════
    // CONSTRUCTOR
    // ═══════════════════════════════════════════════════════════════════════════

    public PaymentPage(Page page) {
        super(page);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // NAVIGATION
    // ═══════════════════════════════════════════════════════════════════════════

    public PaymentPage navigate() {
        page.navigate(baseUrl + "/checkout/payment");
        waitForPageLoad();
        return this;
    }

    @Override
    protected void waitForPageLoad() {
        page.waitForSelector(PAY_NOW_BUTTON,
            new Page.WaitForSelectorOptions().setState(WaitForSelectorState.VISIBLE));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PAYMENT METHOD SELECTION
    // ═══════════════════════════════════════════════════════════════════════════

    public PaymentPage selectPaymentMethod(PaymentMethod method) {
        String locator = String.format(PAYMENT_METHOD_RADIO, method.getValue());
        page.click(locator);

        // Wait for payment form to load based on method
        switch (method) {
            case CREDIT_CARD:
            case DEBIT_CARD:
                page.waitForSelector(CARD_NUMBER_INPUT);
                break;
            case UPI:
                page.waitForSelector(UPI_ID_INPUT);
                break;
            case NET_BANKING:
                page.waitForSelector("[data-testid='bank-list']");
                break;
        }
        return this;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CARD PAYMENT FLOW
    // ═══════════════════════════════════════════════════════════════════════════

    public PaymentPage enterCardNumber(String cardNumber) {
        // Handle iframe if card input is in secure iframe
        FrameLocator cardFrame = page.frameLocator("[name='card-frame']");
        if (cardFrame.locator(CARD_NUMBER_INPUT).count() > 0) {
            cardFrame.locator(CARD_NUMBER_INPUT).fill(cardNumber);
        } else {
            page.fill(CARD_NUMBER_INPUT, cardNumber);
        }
        return this;
    }

    public PaymentPage enterExpiry(String expiry) {
        page.fill(CARD_EXPIRY_INPUT, expiry);
        return this;
    }

    public PaymentPage enterCVV(String cvv) {
        page.fill(CARD_CVV_INPUT, cvv);
        return this;
    }

    public PaymentPage enterCardDetails(TestCard card) {
        return enterCardNumber(card.getNumber())
               .enterExpiry(card.getExpiry())
               .enterCVV(card.getCvv());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PAYMENT SUBMISSION
    // ═══════════════════════════════════════════════════════════════════════════

    public void clickPay() {
        page.click(PAY_NOW_BUTTON);
        // Wait for either success redirect or 3DS popup or error
        page.waitForCondition(() ->
            isOnConfirmationPage() ||
            is3DSPopupVisible() ||
            isPaymentErrorVisible()
        );
    }

    public OrderConfirmationPage payWithCard(TestCard card) {
        enterCardDetails(card);
        clickPay();

        if (card.requires3DS()) {
            handle3DSAuthentication(card.getOtpCode());
        }

        if (isPaymentErrorVisible()) {
            throw new PaymentFailedException(getPaymentError());
        }

        return new OrderConfirmationPage(page);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 3DS AUTHENTICATION
    // ═══════════════════════════════════════════════════════════════════════════

    public PaymentPage handle3DSAuthentication(String otp) {
        // Wait for 3DS iframe
        page.waitForSelector(THREE_DS_IFRAME);

        // Switch to iframe context
        FrameLocator threeDSFrame = page.frameLocator(THREE_DS_IFRAME);

        // Enter OTP
        threeDSFrame.locator(THREE_DS_OTP_INPUT).fill(otp);
        threeDSFrame.locator(THREE_DS_SUBMIT).click();

        // Wait for iframe to close
        page.waitForSelector(THREE_DS_IFRAME,
            new Page.WaitForSelectorOptions().setState(WaitForSelectorState.HIDDEN));

        return this;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // UPI PAYMENT FLOW
    // ═══════════════════════════════════════════════════════════════════════════

    public PaymentPage enterUPIId(String upiId) {
        page.fill(UPI_ID_INPUT, upiId);
        return this;
    }

    public PaymentPage verifyUPIId() {
        page.click(UPI_VERIFY_BUTTON);
        page.waitForSelector(UPI_VERIFIED_BADGE);
        return this;
    }

    public void payWithUPI(String upiId) {
        selectPaymentMethod(PaymentMethod.UPI);
        enterUPIId(upiId);
        verifyUPIId();
        clickPay();
        // UPI payment opens app - handle mock verification in test environment
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // STATE CHECKS
    // ═══════════════════════════════════════════════════════════════════════════

    public boolean isOnConfirmationPage() {
        return page.url().contains("/order/confirmation");
    }

    public boolean is3DSPopupVisible() {
        return page.locator(THREE_DS_IFRAME).isVisible();
    }

    public boolean isPaymentErrorVisible() {
        return page.locator(PAYMENT_ERROR_MESSAGE).isVisible();
    }

    public String getPaymentError() {
        return page.locator(PAYMENT_ERROR_MESSAGE).textContent();
    }

    public boolean isPaymentProcessing() {
        return page.locator(PAYMENT_PROCESSING_LOADER).isVisible();
    }
}
```

---

## 5. Playbook Design

### 5.1 CartSetupPlaybook

```java
/**
 * CartSetupPlaybook - Reusable workflow for setting up cart via API
 *
 * Purpose: Encapsulate all cart setup operations that are reused across tests.
 * This playbook is IMPORTED by tests, not recreated.
 *
 * Benefits:
 * 1. Single source of truth for cart setup
 * 2. If cart API changes, update ONE place
 * 3. Tests focus on their specific scenario
 */
@Component
public class CartSetupPlaybook {

    private final CartApiClient cartApi;
    private final ProductApiClient productApi;
    private final CouponApiClient couponApi;

    @Autowired
    public CartSetupPlaybook(CartApiClient cartApi,
                             ProductApiClient productApi,
                             CouponApiClient couponApi) {
        this.cartApi = cartApi;
        this.productApi = productApi;
        this.couponApi = couponApi;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SIMPLE CART SETUP
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Creates a cart with specified product IDs
     *
     * @param productIds List of product IDs to add
     * @return CartSummary with cart details
     */
    public CartSummary setupCartWithProducts(List<String> productIds) {
        String cartId = cartApi.createCart();

        for (String productId : productIds) {
            cartApi.addItem(cartId, productId, 1);
        }

        return cartApi.getCartSummary(cartId);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CART WITH COUPON
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Creates a cart with products and applies a coupon
     */
    public CartSummary setupCartWithCoupon(List<String> productIds, String couponCode) {
        CartSummary cart = setupCartWithProducts(productIds);

        // Validate coupon is applicable
        CouponValidation validation = couponApi.validate(couponCode, cart.getCartId());
        if (!validation.isValid()) {
            throw new TestSetupException("Coupon not valid: " + validation.getReason());
        }

        cartApi.applyCoupon(cart.getCartId(), couponCode);
        return cartApi.getCartSummary(cart.getCartId());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // COMPLEX CART SCENARIOS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Creates a cart meeting minimum order value for specific tests
     */
    public CartSummary setupCartWithMinimumValue(BigDecimal minimumValue) {
        String cartId = cartApi.createCart();

        // Find products to meet minimum value
        List<Product> products = productApi.getAvailableProducts();
        BigDecimal currentTotal = BigDecimal.ZERO;

        for (Product product : products) {
            if (currentTotal.compareTo(minimumValue) >= 0) {
                break;
            }
            cartApi.addItem(cartId, product.getId(), 1);
            currentTotal = currentTotal.add(product.getPrice());
        }

        CartSummary cart = cartApi.getCartSummary(cartId);
        if (cart.getTotal().compareTo(minimumValue) < 0) {
            throw new TestSetupException(
                "Could not create cart with minimum value: " + minimumValue);
        }

        return cart;
    }

    /**
     * Creates a cart with specific item types (for EMI testing)
     */
    public CartSummary setupEligibleEMICart() {
        String cartId = cartApi.createCart();

        // EMI typically requires high-value items
        List<Product> emiEligible = productApi.getProducts(
            ProductQuery.builder()
                .minPrice(new BigDecimal("10000"))
                .emiEligible(true)
                .build()
        );

        if (emiEligible.isEmpty()) {
            throw new TestSetupException("No EMI-eligible products available");
        }

        cartApi.addItem(cartId, emiEligible.get(0).getId(), 1);
        return cartApi.getCartSummary(cartId);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // MULTI-SELLER CART (Complex Scenario)
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Creates a cart with items from multiple sellers
     * Useful for testing split shipment scenarios
     */
    public CartSummary setupMultiSellerCart(int sellerCount) {
        String cartId = cartApi.createCart();

        // Get products from different sellers
        List<String> usedSellers = new ArrayList<>();
        List<Product> allProducts = productApi.getAvailableProducts();

        for (Product product : allProducts) {
            if (usedSellers.size() >= sellerCount) {
                break;
            }
            if (!usedSellers.contains(product.getSellerId())) {
                cartApi.addItem(cartId, product.getId(), 1);
                usedSellers.add(product.getSellerId());
            }
        }

        if (usedSellers.size() < sellerCount) {
            throw new TestSetupException(
                "Could not find products from " + sellerCount + " different sellers");
        }

        return cartApi.getCartSummary(cartId);
    }
}
```

### 5.2 CheckoutPlaybook

```java
/**
 * CheckoutPlaybook - Reusable UI workflow for checkout process
 *
 * This playbook handles the UI flow from cart to payment page.
 * Used when tests need to get past checkout to focus on payment testing.
 */
@Component
public class CheckoutPlaybook {

    private final Page page;
    private final CheckoutPage checkoutPage;
    private final TestDataProvider testData;

    public CheckoutPlaybook(Page page, TestDataProvider testData) {
        this.page = page;
        this.checkoutPage = new CheckoutPage(page);
        this.testData = testData;
    }

    /**
     * Completes checkout with default test address
     * Gets to payment page ready for payment entry
     */
    public PaymentPage completeCheckoutWithDefaults() {
        checkoutPage.navigate();

        // Use saved address if available, otherwise enter new
        if (checkoutPage.hasSavedAddresses()) {
            checkoutPage.selectFirstSavedAddress();
        } else {
            checkoutPage.enterNewAddress(testData.getDefaultAddress());
        }

        // Select default shipping
        checkoutPage.selectShippingMethod(ShippingMethod.STANDARD);

        // Proceed to payment
        checkoutPage.proceedToPayment();

        return new PaymentPage(page);
    }

    /**
     * Completes checkout with specific address type
     */
    public PaymentPage completeCheckoutWithAddress(AddressType type) {
        checkoutPage.navigate();

        Address address = testData.getAddress(type);
        checkoutPage.enterNewAddress(address);
        checkoutPage.selectShippingMethod(ShippingMethod.STANDARD);
        checkoutPage.proceedToPayment();

        return new PaymentPage(page);
    }

    /**
     * Completes checkout with express shipping
     */
    public PaymentPage completeCheckoutExpress() {
        checkoutPage.navigate();
        checkoutPage.selectFirstSavedAddress();
        checkoutPage.selectShippingMethod(ShippingMethod.EXPRESS);
        checkoutPage.proceedToPayment();

        return new PaymentPage(page);
    }
}
```

---

## 6. Error Handling & Recovery

### 6.1 Payment Error Scenarios

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                          PAYMENT ERROR HANDLING                                          │
├─────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                          │
│  SCENARIO 1: Card Declined                                                              │
│  ─────────────────────────                                                              │
│                                                                                          │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐   │
│  │  @Test(groups = {"payment", "negative"})                                         │   │
│  │  public void handleDeclinedCard() {                                              │   │
│  │      // Setup                                                                    │   │
│  │      cartPlaybook.setupCartWithProducts(List.of("PROD-001"));                    │   │
│  │      checkoutPlaybook.completeCheckoutWithDefaults();                            │   │
│  │                                                                                  │   │
│  │      // Use card that will be declined                                           │   │
│  │      paymentPage.enterCardDetails(TestCards.VISA_DECLINED);                      │   │
│  │      paymentPage.clickPay();                                                     │   │
│  │                                                                                  │   │
│  │      // Verify error handling                                                    │   │
│  │      assertThat(paymentPage.isPaymentErrorVisible()).isTrue();                   │   │
│  │      assertThat(paymentPage.getPaymentError())                                   │   │
│  │          .contains("Card was declined");                                         │   │
│  │                                                                                  │   │
│  │      // Verify user can retry with different card                                │   │
│  │      assertThat(paymentPage.isCardFormEditable()).isTrue();                      │   │
│  │  }                                                                               │   │
│  └─────────────────────────────────────────────────────────────────────────────────┘   │
│                                                                                          │
│  SCENARIO 2: Network Timeout                                                            │
│  ───────────────────────────                                                            │
│                                                                                          │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐   │
│  │  @Test(groups = {"payment", "resilience"})                                       │   │
│  │  public void handlePaymentTimeout() {                                            │   │
│  │      // Setup                                                                    │   │
│  │      cartPlaybook.setupCartWithProducts(List.of("PROD-001"));                    │   │
│  │      checkoutPlaybook.completeCheckoutWithDefaults();                            │   │
│  │                                                                                  │   │
│  │      // Simulate slow network                                                    │   │
│  │      page.route("**/api/payment/**", route -> {                                  │   │
│  │          route.fulfill(new Route.FulfillOptions()                                │   │
│  │              .setStatus(504)                                                     │   │
│  │              .setBody("{\"error\": \"Gateway Timeout\"}"));                      │   │
│  │      });                                                                         │   │
│  │                                                                                  │   │
│  │      paymentPage.enterCardDetails(TestCards.VISA_SUCCESS);                       │   │
│  │      paymentPage.clickPay();                                                     │   │
│  │                                                                                  │   │
│  │      // Verify graceful timeout handling                                         │   │
│  │      assertThat(paymentPage.getPaymentError())                                   │   │
│  │          .contains("Payment is taking longer than expected");                    │   │
│  │      assertThat(paymentPage.isRetryButtonVisible()).isTrue();                    │   │
│  │  }                                                                               │   │
│  └─────────────────────────────────────────────────────────────────────────────────┘   │
│                                                                                          │
│  SCENARIO 3: Session Expiry During Payment                                              │
│  ─────────────────────────────────────────                                              │
│                                                                                          │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐   │
│  │  @Test(groups = {"payment", "session"})                                          │   │
│  │  public void handleSessionExpiryDuringPayment() {                                │   │
│  │      // Setup cart                                                               │   │
│  │      cartPlaybook.setupCartWithProducts(List.of("PROD-001"));                    │   │
│  │      checkoutPlaybook.completeCheckoutWithDefaults();                            │   │
│  │                                                                                  │   │
│  │      // Clear auth cookies to simulate session expiry                            │   │
│  │      context.clearCookies();                                                     │   │
│  │                                                                                  │   │
│  │      // Attempt payment                                                          │   │
│  │      paymentPage.enterCardDetails(TestCards.VISA_SUCCESS);                       │   │
│  │      paymentPage.clickPay();                                                     │   │
│  │                                                                                  │   │
│  │      // Verify redirect to login                                                 │   │
│  │      assertThat(page.url()).contains("/login");                                  │   │
│  │                                                                                  │   │
│  │      // Verify cart is preserved after re-login                                  │   │
│  │      loginPage.login(testUser);                                                  │   │
│  │      assertThat(page.url()).contains("/checkout");                               │   │
│  │  }                                                                               │   │
│  └─────────────────────────────────────────────────────────────────────────────────┘   │
│                                                                                          │
└─────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 7. Network & Console Logging

### 7.1 Capturing Payment Network Calls

```java
/**
 * PaymentNetworkLogger - Captures all payment-related API calls
 *
 * Purpose: Debug failing payments by having full network trace
 */
public class PaymentNetworkLogger {

    private final List<NetworkRequest> capturedRequests = new ArrayList<>();
    private final List<NetworkResponse> capturedResponses = new ArrayList<>();

    public void attachToPage(Page page) {
        // Capture requests
        page.onRequest(request -> {
            if (isPaymentRelated(request.url())) {
                capturedRequests.add(new NetworkRequest(
                    request.url(),
                    request.method(),
                    request.postData(),
                    Instant.now()
                ));
            }
        });

        // Capture responses
        page.onResponse(response -> {
            if (isPaymentRelated(response.url())) {
                capturedResponses.add(new NetworkResponse(
                    response.url(),
                    response.status(),
                    safeGetBody(response),
                    Instant.now()
                ));
            }
        });
    }

    private boolean isPaymentRelated(String url) {
        return url.contains("/payment") ||
               url.contains("/checkout") ||
               url.contains("/order") ||
               url.contains("stripe.com") ||
               url.contains("razorpay.com");
    }

    public void attachToAllureReport() {
        String networkLog = formatNetworkLog();
        Allure.addAttachment("Payment Network Log", "text/plain", networkLog);
    }

    private String formatNetworkLog() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== PAYMENT NETWORK TRACE ===\n\n");

        for (NetworkRequest req : capturedRequests) {
            sb.append(String.format("[%s] %s %s\n",
                req.getTimestamp(), req.getMethod(), req.getUrl()));
            if (req.getBody() != null) {
                sb.append("  Body: ").append(req.getBody()).append("\n");
            }

            // Find matching response
            capturedResponses.stream()
                .filter(r -> r.getUrl().equals(req.getUrl()))
                .findFirst()
                .ifPresent(resp -> {
                    sb.append(String.format("  → %d: %s\n",
                        resp.getStatus(), truncate(resp.getBody(), 500)));
                });
            sb.append("\n");
        }

        return sb.toString();
    }
}
```

---

## 8. Test Execution Configuration

### 8.1 TestNG Suite Configuration

```xml
<!-- payments-suite.xml -->
<!DOCTYPE suite SYSTEM "https://testng.org/testng-1.0.dtd">
<suite name="Payments Test Suite" parallel="methods" thread-count="5">

    <listeners>
        <listener class-name="io.qameta.allure.testng.AllureTestNg"/>
        <listener class-name="com.framework.listeners.PaymentTestListener"/>
    </listeners>

    <parameter name="browser" value="chromium"/>
    <parameter name="headless" value="true"/>

    <!-- Cart Tests - API Only (Fast) -->
    <test name="Cart API Tests">
        <groups>
            <run>
                <include name="cart"/>
                <include name="api"/>
            </run>
        </groups>
        <classes>
            <class name="com.payments.tests.CartApiTests"/>
        </classes>
    </test>

    <!-- Checkout Tests - Hybrid -->
    <test name="Checkout UI Tests">
        <groups>
            <run>
                <include name="checkout"/>
                <include name="ui"/>
            </run>
        </groups>
        <classes>
            <class name="com.payments.tests.CheckoutTests"/>
        </classes>
    </test>

    <!-- Payment Tests - UI Critical -->
    <test name="Payment Flow Tests">
        <groups>
            <run>
                <include name="payment"/>
                <include name="critical"/>
            </run>
        </groups>
        <classes>
            <class name="com.payments.tests.PaymentTests"/>
            <class name="com.payments.tests.PaymentErrorTests"/>
        </classes>
    </test>

    <!-- E2E Journey Tests -->
    <test name="E2E Payment Journeys">
        <groups>
            <run>
                <include name="e2e"/>
                <include name="journey"/>
            </run>
        </groups>
        <classes>
            <class name="com.payments.tests.E2EPaymentJourneyTests"/>
        </classes>
    </test>

</suite>
```

---

## 9. Key Metrics & Assertions

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                         PAYMENTS TEST ASSERTIONS                                         │
├─────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                          │
│  CART ASSERTIONS:                                                                        │
│  ─────────────────                                                                       │
│  • Cart item count matches expected                                                      │
│  • Cart total calculation is accurate (including tax)                                   │
│  • Coupon discount applied correctly                                                    │
│  • Stock validation prevents over-adding                                                │
│                                                                                          │
│  CHECKOUT ASSERTIONS:                                                                    │
│  ─────────────────────                                                                   │
│  • Address validation works (invalid pincode rejected)                                  │
│  • Shipping cost calculated correctly                                                   │
│  • Delivery estimate shown                                                              │
│  • Order summary matches cart                                                           │
│                                                                                          │
│  PAYMENT ASSERTIONS:                                                                     │
│  ────────────────────                                                                    │
│  • Card validation (Luhn check, expiry)                                                 │
│  • 3DS challenge handled                                                                │
│  • Payment confirmation received                                                        │
│  • Order ID generated                                                                   │
│  • Order status = CONFIRMED in database                                                 │
│                                                                                          │
│  POST-PAYMENT ASSERTIONS:                                                                │
│  ─────────────────────────                                                              │
│  • Invoice generated                                                                    │
│  • Confirmation email sent (via API check)                                              │
│  • Inventory updated                                                                    │
│  • Payment captured in payment gateway                                                  │
│                                                                                          │
└─────────────────────────────────────────────────────────────────────────────────────────┘
```

---

*Document End - Payments LLD v1.0*
