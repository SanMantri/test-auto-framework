package com.framework.domains.payments.playbooks;

import com.framework.core.data.TestDataCache;
import com.framework.domains.payments.api.OrderApiClient;
import com.framework.domains.payments.models.Order;
import com.framework.domains.payments.models.Order.OrderStatus;
import com.framework.domains.payments.models.Order.PaymentStatus;
import io.qameta.allure.Step;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PaymentVerificationPlaybook - Reusable payment verification workflows
 *
 * This playbook provides API-based verification of payment and order states.
 * Used after checkout to validate the backend state matches expectations.
 *
 * Usage:
 *   paymentVerificationPlaybook.verifySuccessfulPayment(orderId, expectedTotal);
 *   paymentVerificationPlaybook.verifyRefundProcessed(orderId);
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentVerificationPlaybook {

    private final OrderApiClient orderApi;

    // ═══════════════════════════════════════════════════════════════════════════
    // PAYMENT SUCCESS VERIFICATION
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Verifies a successful payment completed correctly.
     *
     * @param orderId       Order ID to verify
     * @param expectedTotal Expected order total
     * @return Verified order
     */
    @Step("Verify successful payment for order: {orderId}")
    public Order verifySuccessfulPayment(String orderId, BigDecimal expectedTotal) {
        log.info("Verifying successful payment for order: {}", orderId);

        Order order = orderApi.getOrder(orderId);

        assertThat(order.getStatus())
            .as("Order status should be CONFIRMED or PROCESSING")
            .isIn(OrderStatus.CONFIRMED, OrderStatus.PROCESSING);

        assertThat(order.getPaymentStatus())
            .as("Payment should be completed")
            .isEqualTo(PaymentStatus.PAID);

        assertThat(order.isPaid())
            .as("Order should be marked as paid")
            .isTrue();

        if (expectedTotal != null) {
            assertThat(order.getTotal())
                .as("Order total should match expected")
                .isEqualByComparingTo(expectedTotal);
        }

        log.info("Payment verified successfully: {} - Total: {}", order.getOrderNumber(), order.getTotal());

        return order;
    }

    /**
     * Verifies payment was successful using order number.
     *
     * @param orderNumber Order number to verify
     * @return Verified order
     */
    @Step("Verify successful payment by order number: {orderNumber}")
    public Order verifySuccessfulPaymentByNumber(String orderNumber) {
        log.info("Verifying successful payment for order number: {}", orderNumber);

        Order order = orderApi.getOrderByNumber(orderNumber);
        return verifySuccessfulPayment(order.getId(), null);
    }

    /**
     * Waits for and verifies successful payment.
     *
     * @param orderId         Order ID
     * @param maxWaitSeconds  Maximum wait time
     * @return Verified order
     */
    @Step("Wait and verify payment for order: {orderId}")
    public Order waitAndVerifyPayment(String orderId, int maxWaitSeconds) {
        log.info("Waiting for payment confirmation on order: {}", orderId);

        // Wait for order to be confirmed
        Order order = orderApi.waitForStatus(orderId, OrderStatus.CONFIRMED, maxWaitSeconds);

        // Verify payment status
        assertThat(order.getPaymentStatus())
            .as("Payment should be completed after confirmation")
            .isEqualTo(PaymentStatus.PAID);

        return order;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PAYMENT FAILURE VERIFICATION
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Verifies a payment was declined correctly.
     *
     * @param orderId Order ID to verify
     * @return Order with failed payment
     */
    @Step("Verify payment was declined for order: {orderId}")
    public Order verifyPaymentDeclined(String orderId) {
        log.info("Verifying payment was declined for order: {}", orderId);

        Order order = orderApi.getOrder(orderId);

        assertThat(order.getPaymentStatus())
            .as("Payment should be failed")
            .isIn(PaymentStatus.FAILED, PaymentStatus.DECLINED);

        assertThat(order.isPaid())
            .as("Order should not be marked as paid")
            .isFalse();

        log.info("Payment decline verified: {} - Status: {}", order.getOrderNumber(), order.getPaymentStatus());

        return order;
    }

    /**
     * Verifies payment is pending (for async payment methods).
     *
     * @param orderId Order ID to verify
     * @return Order with pending payment
     */
    @Step("Verify payment is pending for order: {orderId}")
    public Order verifyPaymentPending(String orderId) {
        log.info("Verifying payment is pending for order: {}", orderId);

        Order order = orderApi.getOrder(orderId);

        assertThat(order.getPaymentStatus())
            .as("Payment should be pending")
            .isEqualTo(PaymentStatus.PENDING);

        assertThat(order.getStatus())
            .as("Order should be pending payment")
            .isEqualTo(OrderStatus.PENDING_PAYMENT);

        return order;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // REFUND VERIFICATION
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Verifies a refund was processed successfully.
     *
     * @param orderId Order ID to verify
     * @return Refunded order
     */
    @Step("Verify refund processed for order: {orderId}")
    public Order verifyRefundProcessed(String orderId) {
        log.info("Verifying refund for order: {}", orderId);

        Order order = orderApi.getOrder(orderId);

        assertThat(order.getPaymentStatus())
            .as("Payment status should show refund")
            .isIn(PaymentStatus.REFUNDED, PaymentStatus.PARTIALLY_REFUNDED);

        log.info("Refund verified: {} - Status: {}", order.getOrderNumber(), order.getPaymentStatus());

        return order;
    }

    /**
     * Requests and verifies a refund.
     *
     * @param orderId Order ID to refund
     * @return Refund response
     */
    @Step("Request and verify refund for order: {orderId}")
    public OrderApiClient.RefundResponse requestAndVerifyRefund(String orderId) {
        log.info("Requesting refund for order: {}", orderId);

        // Request refund
        OrderApiClient.RefundResponse refundResponse = orderApi.requestRefund(orderId);

        assertThat(refundResponse.status)
            .as("Refund should be processed or pending")
            .isIn("PROCESSED", "PENDING", "INITIATED");

        // Wait a moment for refund to process
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Verify order reflects refund
        verifyRefundProcessed(orderId);

        log.info("Refund completed: {} - Amount: {}", refundResponse.refundId, refundResponse.amount);

        return refundResponse;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ORDER CANCELLATION VERIFICATION
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Verifies order was cancelled successfully.
     *
     * @param orderId Order ID to verify
     * @return Cancelled order
     */
    @Step("Verify order was cancelled: {orderId}")
    public Order verifyOrderCancelled(String orderId) {
        log.info("Verifying order cancellation: {}", orderId);

        Order order = orderApi.getOrder(orderId);

        assertThat(order.getStatus())
            .as("Order should be cancelled")
            .isEqualTo(OrderStatus.CANCELLED);

        log.info("Cancellation verified: {}", order.getOrderNumber());

        return order;
    }

    /**
     * Cancels an order and verifies it was cancelled.
     *
     * @param orderId Order ID to cancel
     * @param reason  Cancellation reason
     * @return Cancelled order
     */
    @Step("Cancel and verify order: {orderId}")
    public Order cancelAndVerify(String orderId, String reason) {
        log.info("Cancelling order: {} with reason: {}", orderId, reason);

        Order order = orderApi.cancelOrder(orderId, reason);

        assertThat(order.getStatus())
            .as("Order should be cancelled")
            .isEqualTo(OrderStatus.CANCELLED);

        return order;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // COMPREHENSIVE VERIFICATION
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Performs complete order verification and stores results in test data.
     *
     * @param orderNumber Order number to verify
     * @param testData    Test data cache for storing results
     * @return Verified order
     */
    @Step("Complete order verification: {orderNumber}")
    public Order performCompleteVerification(String orderNumber, TestDataCache testData) {
        log.info("Performing complete verification for order: {}", orderNumber);

        Order order = orderApi.getOrderByNumber(orderNumber);

        // Store all verification results
        testData.put("verifiedOrder", order);
        testData.put("orderId", order.getId());
        testData.put("orderStatus", order.getStatus().name());
        testData.put("paymentStatus", order.getPaymentStatus().name());
        testData.put("orderTotal", order.getTotal());
        testData.put("isPaid", order.isPaid());
        testData.put("isConfirmed", order.isConfirmed());

        // Verify basic requirements
        assertThat(order.isConfirmed())
            .as("Order should be confirmed")
            .isTrue();

        assertThat(order.getItems())
            .as("Order should have items")
            .isNotEmpty();

        assertThat(order.getShippingAddress())
            .as("Order should have shipping address")
            .isNotNull();

        log.info("Complete verification passed for order: {}", orderNumber);

        return order;
    }

    /**
     * Verifies order matches expected cart values.
     *
     * @param orderId            Order to verify
     * @param expectedItemCount  Expected number of items
     * @param expectedTotal      Expected total
     */
    @Step("Verify order matches cart: items={expectedItemCount}, total={expectedTotal}")
    public void verifyOrderMatchesCart(String orderId, int expectedItemCount, BigDecimal expectedTotal) {
        log.info("Verifying order matches cart expectations");

        Order order = orderApi.getOrder(orderId);

        assertThat(order.getItems().size())
            .as("Order item count should match cart")
            .isEqualTo(expectedItemCount);

        assertThat(order.getTotal())
            .as("Order total should match cart total")
            .isEqualByComparingTo(expectedTotal);
    }
}
