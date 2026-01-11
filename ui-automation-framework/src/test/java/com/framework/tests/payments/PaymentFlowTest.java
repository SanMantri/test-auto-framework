package com.framework.tests.payments;

import com.framework.core.base.BaseTest;
import com.framework.core.data.TestDataCache;
import com.framework.domains.payments.api.CartApiClient;
import com.framework.domains.payments.api.OrderApiClient;
import com.framework.domains.payments.models.Cart;
import com.framework.domains.payments.models.Order;
import com.framework.domains.payments.models.PaymentMethod;
import com.framework.domains.payments.models.TestCard;
import com.framework.domains.payments.pages.PaymentPage;
import io.qameta.allure.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PaymentFlowTest - End-to-end payment flow tests
 *
 * Tests follow the hybrid approach:
 * 1. Setup via API (create cart, add items)
 * 2. Execute payment via UI
 * 3. Verify via API (check order status)
 */
@Epic("E-Commerce Platform")
@Feature("Payment Processing")
public class PaymentFlowTest extends BaseTest {

    @Autowired
    private CartApiClient cartApi;

    @Autowired
    private OrderApiClient orderApi;

    private PaymentPage paymentPage;
    private TestDataCache testData;

    @BeforeMethod
    public void setupTest() {
        testData = getTestData();
        paymentPage = new PaymentPage(getPage());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SUCCESSFUL PAYMENT TESTS
    // ═══════════════════════════════════════════════════════════════════════════

    @Test(description = "Complete payment with Visa card")
    @Story("Credit Card Payments")
    @Severity(SeverityLevel.CRITICAL)
    public void testSuccessfulVisaPayment() {
        // Setup: Create cart with items via API
        Cart cart = cartApi.createCart();
        cartApi.addItem("PROD-001", 2);
        cartApi.addItem("PROD-002", 1);
        testData.set("cartId", cart.getId());

        // Navigate to payment page
        paymentPage.navigate();

        // Execute: Complete payment via UI
        var confirmationPage = paymentPage.payWithCard(TestCard.VISA_SUCCESS);

        // Verify: Order created successfully via API
        String orderId = confirmationPage.getOrderId();
        Order order = orderApi.getOrder(orderId);

        assertThat(order.isPaid()).isTrue();
        assertThat(order.getStatus()).isEqualTo(Order.OrderStatus.CONFIRMED);
        assertThat(order.getPaymentMethod()).isEqualTo("credit_card");
    }

    @Test(description = "Complete payment with Mastercard")
    @Story("Credit Card Payments")
    @Severity(SeverityLevel.CRITICAL)
    public void testSuccessfulMastercardPayment() {
        // Setup via API
        Cart cart = cartApi.createCart();
        cartApi.addItem("PROD-003", 1);
        testData.set("cartId", cart.getId());

        // Execute payment
        paymentPage.navigate();
        var confirmationPage = paymentPage.payWithCard(TestCard.MASTERCARD_SUCCESS);

        // Verify order
        String orderId = confirmationPage.getOrderId();
        Order order = orderApi.getOrder(orderId);

        assertThat(order.isPaid()).isTrue();
        assertThat(order.isConfirmed()).isTrue();
    }

    @Test(description = "Complete payment with AMEX card")
    @Story("Credit Card Payments")
    @Severity(SeverityLevel.NORMAL)
    public void testSuccessfulAmexPayment() {
        // Setup
        cartApi.createCart();
        cartApi.addItem("PROD-001", 1);

        // Execute
        paymentPage.navigate();
        var confirmationPage = paymentPage.payWithCard(TestCard.AMEX_SUCCESS);

        // Verify
        String orderId = confirmationPage.getOrderId();
        Order order = orderApi.getOrder(orderId);

        assertThat(order.isPaid()).isTrue();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 3DS AUTHENTICATION TESTS
    // ═══════════════════════════════════════════════════════════════════════════

    @Test(description = "Complete payment with 3DS authentication")
    @Story("3DS Authentication")
    @Severity(SeverityLevel.CRITICAL)
    public void testPaymentWith3DSAuthentication() {
        // Setup
        cartApi.createCart();
        cartApi.addItem("PROD-001", 1);

        // Navigate to payment
        paymentPage.navigate();
        paymentPage.selectPaymentMethod(PaymentMethod.CREDIT_CARD);
        paymentPage.enterCardDetails(TestCard.VISA_3DS);
        paymentPage.clickPayNow();

        // Verify 3DS popup appears
        assertThat(paymentPage.is3DSPopupVisible())
            .as("3DS authentication popup should be visible")
            .isTrue();

        // Complete 3DS
        paymentPage.handle3DSAuthentication(TestCard.VISA_3DS.getOtpCode());

        // Verify success
        assertThat(paymentPage.isPaymentSuccessful()).isTrue();
    }

    @Test(description = "Complete payment with Mastercard 3DS")
    @Story("3DS Authentication")
    @Severity(SeverityLevel.NORMAL)
    public void testMastercard3DSPayment() {
        // Setup
        cartApi.createCart();
        cartApi.addItem("PROD-002", 2);

        // Execute
        paymentPage.navigate();
        var confirmationPage = paymentPage.payWithCard(TestCard.MASTERCARD_3DS);

        // Verify
        String orderId = confirmationPage.getOrderId();
        Order order = orderApi.getOrder(orderId);

        assertThat(order.isPaid()).isTrue();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DECLINED PAYMENT TESTS
    // ═══════════════════════════════════════════════════════════════════════════

    @Test(description = "Handle declined card payment")
    @Story("Payment Errors")
    @Severity(SeverityLevel.CRITICAL)
    public void testDeclinedCardPayment() {
        // Setup
        cartApi.createCart();
        cartApi.addItem("PROD-001", 1);

        // Execute with declined card
        paymentPage.navigate();
        paymentPage.selectPaymentMethod(PaymentMethod.CREDIT_CARD);
        paymentPage.enterCardDetails(TestCard.VISA_DECLINED);
        paymentPage.clickPayNow();

        // Verify error message
        assertThat(paymentPage.isPaymentErrorVisible())
            .as("Payment error should be displayed")
            .isTrue();

        assertThat(paymentPage.getPaymentError())
            .containsIgnoringCase("declined");
    }

    @Test(description = "Handle insufficient funds")
    @Story("Payment Errors")
    @Severity(SeverityLevel.NORMAL)
    public void testInsufficientFundsPayment() {
        // Setup
        cartApi.createCart();
        cartApi.addItem("PROD-001", 10);  // Large quantity

        // Execute
        paymentPage.navigate();
        paymentPage.selectPaymentMethod(PaymentMethod.CREDIT_CARD);
        paymentPage.enterCardDetails(TestCard.VISA_INSUFFICIENT_FUNDS);
        paymentPage.clickPayNow();

        // Verify
        assertThat(paymentPage.isPaymentErrorVisible()).isTrue();
        assertThat(paymentPage.getPaymentError())
            .containsIgnoringCase("insufficient");
    }

    @Test(description = "Handle expired card")
    @Story("Payment Errors")
    @Severity(SeverityLevel.NORMAL)
    public void testExpiredCardPayment() {
        // Setup
        cartApi.createCart();
        cartApi.addItem("PROD-001", 1);

        // Execute
        paymentPage.navigate();
        paymentPage.selectPaymentMethod(PaymentMethod.CREDIT_CARD);
        paymentPage.enterCardDetails(TestCard.EXPIRED_CARD);
        paymentPage.clickPayNow();

        // Verify
        assertThat(paymentPage.isPaymentErrorVisible()).isTrue();
        assertThat(paymentPage.getPaymentError())
            .containsIgnoringCase("expired");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // UPI PAYMENT TESTS
    // ═══════════════════════════════════════════════════════════════════════════

    @Test(description = "Complete UPI payment")
    @Story("UPI Payments")
    @Severity(SeverityLevel.CRITICAL)
    public void testSuccessfulUPIPayment() {
        // Setup
        cartApi.createCart();
        cartApi.addItem("PROD-001", 1);

        // Execute UPI payment
        paymentPage.navigate();
        paymentPage.selectPaymentMethod(PaymentMethod.UPI);
        paymentPage.enterUPIId("test@upi");
        paymentPage.verifyUPIId();

        // Verify UPI ID validated
        assertThat(paymentPage.isUPIVerified())
            .as("UPI ID should be verified")
            .isTrue();

        paymentPage.clickPayNow();

        // Verify payment processing
        assertThat(paymentPage.isPaymentProcessing() || paymentPage.isPaymentSuccessful())
            .isTrue();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // COUPON TESTS
    // ═══════════════════════════════════════════════════════════════════════════

    @Test(description = "Apply valid coupon and complete payment")
    @Story("Coupons")
    @Severity(SeverityLevel.NORMAL)
    public void testPaymentWithCoupon() {
        // Setup cart with coupon
        Cart cart = cartApi.createCart();
        cartApi.addItem("PROD-001", 2);
        Cart cartWithCoupon = cartApi.applyCoupon("DISCOUNT10");
        testData.set("originalTotal", cart.getTotal());
        testData.set("discountedTotal", cartWithCoupon.getTotal());

        // Execute payment
        paymentPage.navigate();

        // Verify discounted total shown
        String displayedTotal = paymentPage.getOrderTotal();
        assertThat(displayedTotal)
            .as("Discounted total should be displayed")
            .isNotNull();

        // Complete payment
        var confirmationPage = paymentPage.payWithCard(TestCard.VISA_SUCCESS);

        // Verify order with discount
        String orderId = confirmationPage.getOrderId();
        Order order = orderApi.getOrder(orderId);

        assertThat(order.getDiscountAmount())
            .as("Order should have discount applied")
            .isGreaterThan(java.math.BigDecimal.ZERO);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DATA-DRIVEN TESTS
    // ═══════════════════════════════════════════════════════════════════════════

    @DataProvider(name = "testCards")
    public Object[][] testCards() {
        return new Object[][] {
            { TestCard.VISA_SUCCESS, true, null },
            { TestCard.MASTERCARD_SUCCESS, true, null },
            { TestCard.AMEX_SUCCESS, true, null },
            { TestCard.VISA_DECLINED, false, "declined" },
            { TestCard.VISA_INSUFFICIENT_FUNDS, false, "insufficient" }
        };
    }

    @Test(dataProvider = "testCards", description = "Test various card scenarios")
    @Story("Credit Card Payments")
    @Severity(SeverityLevel.NORMAL)
    public void testCardPaymentScenarios(TestCard card, boolean shouldSucceed, String errorMessage) {
        // Setup
        cartApi.createCart();
        cartApi.addItem("PROD-001", 1);

        // Execute
        paymentPage.navigate();
        paymentPage.selectPaymentMethod(PaymentMethod.CREDIT_CARD);
        paymentPage.enterCardDetails(card);
        paymentPage.clickPayNow();

        // Handle 3DS if needed
        if (card.requires3DS() && paymentPage.is3DSPopupVisible()) {
            paymentPage.handle3DSAuthentication(card.getOtpCode());
        }

        // Verify
        if (shouldSucceed) {
            assertThat(paymentPage.isPaymentSuccessful() || !paymentPage.isPaymentErrorVisible())
                .as("Payment should succeed for " + card.getNumber())
                .isTrue();
        } else {
            assertThat(paymentPage.isPaymentErrorVisible())
                .as("Payment should fail for " + card.getNumber())
                .isTrue();
            if (errorMessage != null) {
                assertThat(paymentPage.getPaymentError().toLowerCase())
                    .contains(errorMessage);
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ORDER VERIFICATION TESTS
    // ═══════════════════════════════════════════════════════════════════════════

    @Test(description = "Verify order details after successful payment")
    @Story("Order Verification")
    @Severity(SeverityLevel.CRITICAL)
    public void testOrderDetailsAfterPayment() {
        // Setup with specific items
        Cart cart = cartApi.createCart();
        cartApi.addItem("PROD-001", 2);
        cartApi.addItem("PROD-002", 1);
        testData.set("expectedItemCount", 3);
        testData.set("expectedTotal", cart.getTotal());

        // Execute
        paymentPage.navigate();
        var confirmationPage = paymentPage.payWithCard(TestCard.VISA_SUCCESS);

        // Verify order details via API
        String orderId = confirmationPage.getOrderId();
        Order order = orderApi.getOrder(orderId);

        assertThat(order.getItems()).hasSize(2);
        assertThat(order.getTotalQuantity()).isEqualTo(3);
        assertThat(order.isPaid()).isTrue();
        assertThat(order.getPaymentDetails()).isNotNull();
        assertThat(order.getPaymentDetails().getStatus()).isEqualTo("captured");
    }

    @Test(description = "Verify order status transitions")
    @Story("Order Verification")
    @Severity(SeverityLevel.NORMAL)
    public void testOrderStatusTransitions() {
        // Setup
        cartApi.createCart();
        cartApi.addItem("PROD-001", 1);

        // Execute
        paymentPage.navigate();
        var confirmationPage = paymentPage.payWithCard(TestCard.VISA_SUCCESS);

        String orderId = confirmationPage.getOrderId();

        // Wait for order to be confirmed
        Order order = orderApi.waitForStatus(orderId, Order.OrderStatus.CONFIRMED, 30);

        assertThat(order.getStatus()).isEqualTo(Order.OrderStatus.CONFIRMED);

        // Verify order is confirmed
        assertThat(orderApi.isOrderConfirmed(orderId)).isTrue();
    }
}
