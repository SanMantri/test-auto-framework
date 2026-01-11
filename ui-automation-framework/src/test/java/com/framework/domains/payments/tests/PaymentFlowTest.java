package com.framework.domains.payments.tests;

import com.framework.core.base.BaseTest;
import com.framework.domains.payments.api.CartApiClient;
import com.framework.domains.payments.api.OrderApiClient;
import com.framework.domains.payments.models.Cart;
import com.framework.domains.payments.models.Order;
import com.framework.domains.payments.models.Order.OrderStatus;
import com.framework.domains.payments.models.PaymentMethod;
import com.framework.domains.payments.models.TestCard;
import com.framework.domains.payments.pages.CartPage;
import com.framework.domains.payments.pages.CheckoutPage;
import com.framework.domains.payments.pages.CheckoutPage.ShippingMethod;
import com.framework.domains.payments.pages.OrderConfirmationPage;
import com.framework.domains.payments.pages.PaymentPage;
import com.framework.domains.payments.playbooks.CartSetupPlaybook;
import com.framework.domains.payments.playbooks.CheckoutPlaybook;
import com.framework.domains.payments.playbooks.PaymentVerificationPlaybook;
import io.qameta.allure.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PaymentFlowTest - End-to-end payment flow tests
 *
 * Tests the complete checkout and payment experience using:
 * - API-driven test setup (cart creation)
 * - UI interactions for critical checkout path
 * - API verification of order state
 *
 * Test Strategy:
 * - Happy path: Card, UPI, COD payments
 * - 3DS authentication flow
 * - Payment failures: Declined, insufficient funds
 * - Coupon and discount application
 */
@Epic("Payments")
@Feature("Checkout Flow")
public class PaymentFlowTest extends BaseTest {

    @Autowired
    private CartSetupPlaybook cartSetupPlaybook;

    @Autowired
    private CheckoutPlaybook checkoutPlaybook;

    @Autowired
    private PaymentVerificationPlaybook paymentVerificationPlaybook;

    @Autowired
    private CartApiClient cartApi;

    @Autowired
    private OrderApiClient orderApi;

    // ═══════════════════════════════════════════════════════════════════════════
    // SUCCESSFUL PAYMENT TESTS
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @Severity(SeverityLevel.BLOCKER)
    @Story("Card Payment")
    @Description("Verify successful checkout with Visa card")
    public void testSuccessfulCardPayment() {
        // API: Setup cart
        Cart cart = cartSetupPlaybook.setupCartWithSingleItem("PROD-001");
        testData().put("cart", cart);

        // UI: Complete checkout
        OrderConfirmationPage confirmationPage = checkoutPlaybook.completeCheckoutWithDefaults(
            getPage(), TestCard.VISA_SUCCESS);

        // Verify confirmation page
        assertThat(confirmationPage.isOrderConfirmed())
            .as("Order should be confirmed")
            .isTrue();

        String orderNumber = confirmationPage.getOrderNumber();
        testData().put("orderNumber", orderNumber);

        // API: Verify order
        Order order = paymentVerificationPlaybook.verifySuccessfulPaymentByNumber(orderNumber);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @Story("Card Payment")
    @Description("Verify successful checkout with Mastercard")
    public void testSuccessfulMastercardPayment() {
        // API: Setup cart
        Cart cart = cartSetupPlaybook.setupCartWithSingleItem("PROD-002", 2);
        testData().put("cartTotal", cart.getTotal());

        // UI: Complete checkout
        OrderConfirmationPage confirmationPage = checkoutPlaybook.completeCheckoutWithDefaults(
            getPage(), TestCard.MASTERCARD_SUCCESS);

        // Verify
        String orderNumber = confirmationPage.getOrderNumber();
        Order order = orderApi.getOrderByNumber(orderNumber);

        assertThat(order.isPaid()).isTrue();
        assertThat(order.getTotal()).isEqualByComparingTo(cart.getTotal());
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @Story("Card Payment")
    @Description("Verify successful checkout with AMEX card")
    public void testSuccessfulAmexPayment() {
        // API: Setup cart
        cartSetupPlaybook.setupCartWithSingleItem("PROD-003");

        // UI: Complete checkout
        OrderConfirmationPage confirmationPage = checkoutPlaybook.completeCheckoutWithDefaults(
            getPage(), TestCard.AMEX_SUCCESS);

        // Verify AMEX 4-digit CVV was accepted
        assertThat(confirmationPage.isOrderConfirmed()).isTrue();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 3DS AUTHENTICATION TESTS
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @Story("3DS Authentication")
    @Description("Verify checkout with 3DS authentication flow")
    public void testCheckoutWith3DSAuthentication() {
        // API: Setup cart
        Cart cart = cartSetupPlaybook.setupCartWithSingleItem("PROD-PREMIUM-001");
        testData().put("cart", cart);

        // UI: Complete checkout with 3DS card
        OrderConfirmationPage confirmationPage = checkoutPlaybook.completeCheckoutWith3DS(getPage());

        // Verify 3DS was handled correctly
        assertThat(confirmationPage.isOrderConfirmed())
            .as("Order should be confirmed after 3DS")
            .isTrue();

        // API: Verify payment
        String orderNumber = confirmationPage.getOrderNumber();
        Order order = paymentVerificationPlaybook.verifySuccessfulPaymentByNumber(orderNumber);
        assertThat(order.getPaymentInfo()).isNotNull();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ALTERNATIVE PAYMENT METHODS
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @Story("UPI Payment")
    @Description("Verify checkout with UPI payment")
    public void testCheckoutWithUPI() {
        // API: Setup cart
        cartSetupPlaybook.setupCartWithSingleItem("PROD-001");

        // UI: Complete checkout with UPI
        OrderConfirmationPage confirmationPage = checkoutPlaybook.completeCheckoutWithUPI(
            getPage(), "test@upi", checkoutPlaybook.getDefaultAddress());

        // Verify
        assertThat(confirmationPage.isOrderConfirmed()).isTrue();
        assertThat(confirmationPage.getPaymentMethod()).containsIgnoringCase("UPI");
    }

    @Test
    @Severity(SeverityLevel.NORMAL)
    @Story("COD Payment")
    @Description("Verify checkout with Cash on Delivery")
    public void testCheckoutWithCOD() {
        // API: Setup cart (must be within COD limit)
        cartSetupPlaybook.setupCartWithSingleItem("PROD-SMALL-001");

        // UI: Complete checkout with COD
        OrderConfirmationPage confirmationPage = checkoutPlaybook.completeCheckoutWithCOD(
            getPage(), checkoutPlaybook.getDefaultAddress());

        // Verify COD order
        assertThat(confirmationPage.isOrderConfirmed()).isTrue();
        assertThat(confirmationPage.getPaymentMethod()).containsIgnoringCase("Cash");

        // API: Verify order is pending payment (COD)
        String orderNumber = confirmationPage.getOrderNumber();
        Order order = orderApi.getOrderByNumber(orderNumber);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // COUPON AND DISCOUNT TESTS
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @Severity(SeverityLevel.NORMAL)
    @Story("Discounts")
    @Description("Verify checkout with coupon discount applied")
    public void testCheckoutWithCoupon() {
        // API: Setup cart with coupon
        Cart cart = cartSetupPlaybook.setupCartWithCoupon("PROD-001", "SAVE10");
        BigDecimal expectedTotal = cart.getTotal();
        testData().put("expectedTotal", expectedTotal);

        // Verify coupon was applied
        assertThat(cart.getDiscount()).isGreaterThan(BigDecimal.ZERO);

        // UI: Complete checkout
        OrderConfirmationPage confirmationPage = checkoutPlaybook.completeCheckoutWithDefaults(
            getPage(), TestCard.VISA_SUCCESS);

        // Verify discount reflected in order
        String orderNumber = confirmationPage.getOrderNumber();
        Order order = orderApi.getOrderByNumber(orderNumber);

        assertThat(order.getDiscount()).isGreaterThan(BigDecimal.ZERO);
        assertThat(order.getTotal()).isEqualByComparingTo(expectedTotal);
    }

    @Test
    @Severity(SeverityLevel.NORMAL)
    @Story("Discounts")
    @Description("Verify free shipping applied when threshold is met")
    public void testFreeShippingThreshold() {
        // API: Setup cart meeting free shipping threshold
        Cart cart = cartSetupPlaybook.setupFreeShippingCart();

        // UI: Navigate to checkout and verify shipping
        CartPage cartPage = new CartPage(getPage());
        cartPage.navigate();

        CheckoutPage checkoutPage = cartPage.proceedToCheckout();
        checkoutPage.selectSavedAddress(getSavedAddressId());
        checkoutPage.continueToNextStep();

        // Verify free shipping option is available
        assertThat(checkoutPage.isShippingMethodAvailable(ShippingMethod.FREE)).isTrue();
        assertThat(checkoutPage.getShippingPrice(ShippingMethod.FREE)).contains("0");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PAYMENT FAILURE TESTS
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @Story("Payment Failures")
    @Description("Verify appropriate error when card is declined")
    public void testDeclinedCardPayment() {
        // API: Setup cart
        cartSetupPlaybook.setupCartWithSingleItem("PROD-001");

        // UI: Attempt checkout with declined card
        PaymentPage paymentPage = checkoutPlaybook.attemptCheckoutWithDeclinedCard(getPage());

        // Verify error is displayed
        assertThat(paymentPage.isPaymentErrorVisible())
            .as("Payment error should be displayed")
            .isTrue();

        String errorMessage = paymentPage.getPaymentError();
        assertThat(errorMessage)
            .as("Error message should indicate declined")
            .containsIgnoringCase("declined");
    }

    @Test
    @Severity(SeverityLevel.NORMAL)
    @Story("Payment Failures")
    @Description("Verify appropriate error for insufficient funds")
    public void testInsufficientFundsPayment() {
        // API: Setup high-value cart
        cartSetupPlaybook.setupHighValueCart();

        // UI: Attempt checkout with insufficient funds card
        PaymentPage paymentPage = checkoutPlaybook.attemptCheckoutWithInsufficientFunds(getPage());

        // Verify error
        assertThat(paymentPage.isPaymentErrorVisible()).isTrue();
        assertThat(paymentPage.getPaymentError())
            .containsIgnoringCase("insufficient");
    }

    @Test
    @Severity(SeverityLevel.NORMAL)
    @Story("Payment Failures")
    @Description("Verify expired card is rejected")
    public void testExpiredCardRejection() {
        // API: Setup cart
        cartSetupPlaybook.setupMinimalCart();

        // UI: Navigate to payment
        CartPage cartPage = new CartPage(getPage());
        cartPage.navigate();
        CheckoutPage checkoutPage = cartPage.proceedToCheckout();
        PaymentPage paymentPage = checkoutPage.completeCheckoutWithNewAddress(
            checkoutPlaybook.getDefaultAddress(), ShippingMethod.STANDARD);

        // Enter expired card
        paymentPage.selectPaymentMethod(PaymentMethod.CREDIT_CARD);
        paymentPage.enterCardDetails(TestCard.EXPIRED_CARD);
        paymentPage.clickPayNow();

        // Verify rejection
        assertThat(paymentPage.isPaymentErrorVisible()).isTrue();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // MULTI-ITEM CART TESTS
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @Severity(SeverityLevel.NORMAL)
    @Story("Multi-Item Cart")
    @Description("Verify checkout with multiple items in cart")
    public void testCheckoutWithMultipleItems() {
        // API: Setup cart with multiple items
        List<String> products = List.of("PROD-001", "PROD-002", "PROD-003");
        Cart cart = cartSetupPlaybook.setupCartWithItems(products);

        assertThat(cart.getItemCount()).isEqualTo(3);

        // UI: Complete checkout
        OrderConfirmationPage confirmationPage = checkoutPlaybook.completeCheckoutWithDefaults(
            getPage(), TestCard.VISA_SUCCESS);

        // Verify all items in order
        String orderNumber = confirmationPage.getOrderNumber();
        Order order = orderApi.getOrderByNumber(orderNumber);

        assertThat(order.getItems()).hasSize(3);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ORDER CANCELLATION AND REFUND TESTS
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @Severity(SeverityLevel.NORMAL)
    @Story("Order Management")
    @Description("Verify order can be cancelled and refunded")
    public void testOrderCancellationAndRefund() {
        // Complete a successful purchase first
        cartSetupPlaybook.setupCartWithSingleItem("PROD-001");
        OrderConfirmationPage confirmationPage = checkoutPlaybook.completeCheckoutWithDefaults(
            getPage(), TestCard.VISA_SUCCESS);

        String orderNumber = confirmationPage.getOrderNumber();
        Order order = orderApi.getOrderByNumber(orderNumber);

        // API: Cancel and request refund
        order = paymentVerificationPlaybook.cancelAndVerify(order.getId(), "Changed mind");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);

        // Request refund
        OrderApiClient.RefundResponse refund = paymentVerificationPlaybook.requestAndVerifyRefund(order.getId());
        assertThat(refund.status).isIn("PROCESSED", "PENDING", "INITIATED");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // HELPER METHODS
    // ═══════════════════════════════════════════════════════════════════════════

    private String getSavedAddressId() {
        // Get from global data or use default
        String addressId = (String) globalData().get("defaultAddressId");
        return addressId != null ? addressId : "ADDR-001";
    }
}
