package com.framework.domains.payments.playbooks;

import com.framework.core.data.TestDataCache;
import com.framework.domains.payments.api.OrderApiClient;
import com.framework.domains.payments.models.Order;
import com.framework.domains.payments.models.Order.Address;
import com.framework.domains.payments.models.PaymentMethod;
import com.framework.domains.payments.models.TestCard;
import com.framework.domains.payments.pages.CartPage;
import com.framework.domains.payments.pages.CheckoutPage;
import com.framework.domains.payments.pages.CheckoutPage.ShippingMethod;
import com.framework.domains.payments.pages.OrderConfirmationPage;
import com.framework.domains.payments.pages.PaymentPage;
import com.microsoft.playwright.Page;
import io.qameta.allure.Step;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * CheckoutPlaybook - Reusable checkout workflow compositions
 *
 * This playbook provides complete checkout flows that can be imported into tests.
 * It combines API setup with UI interactions for the critical checkout path.
 *
 * Usage:
 *   OrderConfirmationPage confirmation = checkoutPlaybook.completeCheckout(page, TestCard.VISA_SUCCESS);
 *   String orderNumber = confirmation.getOrderNumber();
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CheckoutPlaybook {

    private final CartSetupPlaybook cartSetupPlaybook;
    private final OrderApiClient orderApi;

    // ═══════════════════════════════════════════════════════════════════════════
    // STANDARD CHECKOUT FLOWS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Completes a standard checkout flow with card payment.
     * Assumes cart is already set up.
     *
     * @param page     Playwright page
     * @param card     Test card to use
     * @param address  Delivery address
     * @param shipping Shipping method
     * @return Order confirmation page
     */
    @Step("Complete checkout with card payment")
    public OrderConfirmationPage completeCheckout(Page page, TestCard card, Address address, ShippingMethod shipping) {
        log.info("Starting checkout flow with card payment");

        // Start from cart
        CartPage cartPage = new CartPage(page);
        cartPage.navigate();

        // Proceed to checkout
        CheckoutPage checkoutPage = cartPage.proceedToCheckout();

        // Complete address and shipping
        PaymentPage paymentPage = checkoutPage.completeCheckoutWithNewAddress(address, shipping);

        // Complete payment
        OrderConfirmationPage confirmationPage = paymentPage.payWithCard(card);

        log.info("Checkout completed. Order: {}", confirmationPage.getOrderNumber());

        return confirmationPage;
    }

    /**
     * Completes checkout with default address and standard shipping.
     *
     * @param page Playwright page
     * @param card Test card to use
     * @return Order confirmation page
     */
    @Step("Complete checkout with defaults")
    public OrderConfirmationPage completeCheckoutWithDefaults(Page page, TestCard card) {
        return completeCheckout(page, card, getDefaultAddress(), ShippingMethod.STANDARD);
    }

    /**
     * Completes checkout using a saved address.
     *
     * @param page      Playwright page
     * @param addressId Saved address ID
     * @param card      Test card to use
     * @param shipping  Shipping method
     * @return Order confirmation page
     */
    @Step("Complete checkout with saved address")
    public OrderConfirmationPage completeCheckoutWithSavedAddress(
            Page page, String addressId, TestCard card, ShippingMethod shipping) {
        log.info("Starting checkout flow with saved address: {}", addressId);

        CartPage cartPage = new CartPage(page);
        cartPage.navigate();

        CheckoutPage checkoutPage = cartPage.proceedToCheckout();
        PaymentPage paymentPage = checkoutPage.completeCheckoutWithSavedAddress(addressId, shipping);
        OrderConfirmationPage confirmationPage = paymentPage.payWithCard(card);

        log.info("Checkout completed. Order: {}", confirmationPage.getOrderNumber());

        return confirmationPage;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // FULL END-TO-END FLOWS (Cart Setup + Checkout)
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Sets up cart via API and completes checkout via UI.
     * This is the primary hybrid testing pattern.
     *
     * @param page      Playwright page
     * @param productId Product to purchase
     * @param card      Test card to use
     * @return Order confirmation page
     */
    @Step("Complete end-to-end purchase flow")
    public OrderConfirmationPage completePurchase(Page page, String productId, TestCard card) {
        log.info("Starting end-to-end purchase flow for product: {}", productId);

        // API: Setup cart
        cartSetupPlaybook.setupCartWithSingleItem(productId);

        // UI: Complete checkout
        return completeCheckoutWithDefaults(page, card);
    }

    /**
     * Sets up cart with coupon and completes checkout.
     *
     * @param page       Playwright page
     * @param productId  Product to purchase
     * @param couponCode Coupon to apply
     * @param card       Test card to use
     * @return Order confirmation page
     */
    @Step("Complete purchase with coupon")
    public OrderConfirmationPage completePurchaseWithCoupon(
            Page page, String productId, String couponCode, TestCard card) {
        log.info("Starting purchase flow with coupon: {}", couponCode);

        // API: Setup cart with coupon
        cartSetupPlaybook.setupCartWithCoupon(productId, couponCode);

        // UI: Complete checkout
        return completeCheckoutWithDefaults(page, card);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PAYMENT METHOD SPECIFIC FLOWS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Completes checkout with UPI payment.
     *
     * @param page    Playwright page
     * @param upiId   UPI ID to use
     * @param address Delivery address
     * @return Order confirmation page
     */
    @Step("Complete checkout with UPI")
    public OrderConfirmationPage completeCheckoutWithUPI(Page page, String upiId, Address address) {
        log.info("Starting checkout flow with UPI: {}", upiId);

        CartPage cartPage = new CartPage(page);
        cartPage.navigate();

        CheckoutPage checkoutPage = cartPage.proceedToCheckout();
        PaymentPage paymentPage = checkoutPage.completeCheckoutWithNewAddress(address, ShippingMethod.STANDARD);

        // UPI payment flow
        paymentPage.selectPaymentMethod(PaymentMethod.UPI);
        paymentPage.enterUPIId(upiId);
        paymentPage.verifyUPIId();
        paymentPage.clickPayNow();

        // Wait for UPI confirmation
        paymentPage.waitForUrl(".*/order/confirmation.*", 60000);

        return new OrderConfirmationPage(page);
    }

    /**
     * Completes checkout with Cash on Delivery.
     *
     * @param page    Playwright page
     * @param address Delivery address
     * @return Order confirmation page
     */
    @Step("Complete checkout with COD")
    public OrderConfirmationPage completeCheckoutWithCOD(Page page, Address address) {
        log.info("Starting checkout flow with COD");

        CartPage cartPage = new CartPage(page);
        cartPage.navigate();

        CheckoutPage checkoutPage = cartPage.proceedToCheckout();
        checkoutPage.completeCheckoutWithNewAddress(address, ShippingMethod.STANDARD);

        // COD doesn't go through PaymentPage, order is placed directly
        return checkoutPage.placeOrder();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 3DS SPECIFIC FLOWS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Completes checkout with 3DS authentication.
     * Uses a card that triggers 3DS.
     *
     * @param page Playwright page
     * @return Order confirmation page
     */
    @Step("Complete checkout with 3DS authentication")
    public OrderConfirmationPage completeCheckoutWith3DS(Page page) {
        return completeCheckoutWithDefaults(page, TestCard.VISA_3DS);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ORDER VERIFICATION
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Completes checkout and verifies order via API.
     *
     * @param page      Playwright page
     * @param productId Product to purchase
     * @param card      Test card to use
     * @param testData  Test data cache to store results
     * @return Verified order
     */
    @Step("Complete purchase and verify order")
    public Order completePurchaseAndVerify(
            Page page, String productId, TestCard card, TestDataCache testData) {
        log.info("Completing purchase with verification");

        // Complete checkout
        OrderConfirmationPage confirmationPage = completePurchase(page, productId, card);
        String orderNumber = confirmationPage.getOrderNumber();

        // Store in test data
        testData.put("orderNumber", orderNumber);
        testData.put("confirmationDisplayed", true);

        // API: Verify order was created correctly
        Order order = orderApi.getOrderByNumber(orderNumber);

        // Store full order details
        testData.put("order", order);
        testData.put("orderId", order.getId());

        log.info("Order verified: {} - Status: {}", order.getOrderNumber(), order.getStatus());

        return order;
    }

    /**
     * Waits for order to reach confirmed status after checkout.
     *
     * @param orderNumber Order number from confirmation page
     * @return Confirmed order
     */
    @Step("Wait for order confirmation")
    public Order waitForOrderConfirmation(String orderNumber) {
        log.info("Waiting for order confirmation: {}", orderNumber);

        Order order = orderApi.getOrderByNumber(orderNumber);
        return orderApi.waitForStatus(order.getId(), Order.OrderStatus.CONFIRMED, 30);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // FAILURE SCENARIO FLOWS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Attempts checkout with a card that will be declined.
     * Used for testing error handling.
     *
     * @param page Playwright page
     * @return PaymentPage for assertion on error state
     */
    @Step("Attempt checkout with declined card")
    public PaymentPage attemptCheckoutWithDeclinedCard(Page page) {
        log.info("Attempting checkout with declined card");

        CartPage cartPage = new CartPage(page);
        cartPage.navigate();

        CheckoutPage checkoutPage = cartPage.proceedToCheckout();
        PaymentPage paymentPage = checkoutPage.completeCheckoutWithNewAddress(
            getDefaultAddress(), ShippingMethod.STANDARD);

        // Use declined card
        paymentPage.selectPaymentMethod(PaymentMethod.CREDIT_CARD);
        paymentPage.enterCardDetails(TestCard.VISA_DECLINED);
        paymentPage.clickPayNow();

        // Wait for error to appear
        paymentPage.waitFor(2000);

        return paymentPage;
    }

    /**
     * Attempts checkout with insufficient funds card.
     *
     * @param page Playwright page
     * @return PaymentPage for assertion
     */
    @Step("Attempt checkout with insufficient funds")
    public PaymentPage attemptCheckoutWithInsufficientFunds(Page page) {
        log.info("Attempting checkout with insufficient funds card");

        CartPage cartPage = new CartPage(page);
        cartPage.navigate();

        CheckoutPage checkoutPage = cartPage.proceedToCheckout();
        PaymentPage paymentPage = checkoutPage.completeCheckoutWithNewAddress(
            getDefaultAddress(), ShippingMethod.STANDARD);

        paymentPage.selectPaymentMethod(PaymentMethod.CREDIT_CARD);
        paymentPage.enterCardDetails(TestCard.VISA_INSUFFICIENT_FUNDS);
        paymentPage.clickPayNow();

        paymentPage.waitFor(2000);

        return paymentPage;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // HELPER METHODS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Returns a default test address.
     */
    public Address getDefaultAddress() {
        return Address.builder()
            .fullName("Test User")
            .phone("9876543210")
            .line1("123 Test Street")
            .line2("Apartment 4B")
            .city("Test City")
            .state("Test State")
            .pincode("123456")
            .country("IN")
            .type("HOME")
            .build();
    }

    /**
     * Returns a business/office address.
     */
    public Address getBusinessAddress() {
        return Address.builder()
            .fullName("Test Business User")
            .phone("9876543210")
            .line1("456 Business Park")
            .line2("Tower A, Floor 10")
            .city("Business City")
            .state("Business State")
            .pincode("654321")
            .country("IN")
            .type("OFFICE")
            .build();
    }
}
