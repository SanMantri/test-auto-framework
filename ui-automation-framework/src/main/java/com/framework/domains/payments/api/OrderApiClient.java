package com.framework.domains.payments.api;

import com.framework.core.base.BaseApiClient;
import com.framework.domains.payments.models.Order;
import com.framework.domains.payments.models.Order.OrderStatus;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * OrderApiClient - API operations for orders
 *
 * Used for:
 * - Verifying order creation after payment
 * - Order status checks
 * - Cancellation and refunds
 */
@Slf4j
@Component
public class OrderApiClient extends BaseApiClient {

    @Override
    protected String getBasePath() {
        return "/api/v1/orders";
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ORDER QUERIES
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Gets an order by ID.
     */
    public Order getOrder(String orderId) {
        log.info("Getting order: {}", orderId);
        Response response = get("/" + orderId);
        return getOkAs(response, Order.class);
    }

    /**
     * Gets all orders for the authenticated user.
     */
    public List<Order> getMyOrders() {
        log.info("Getting my orders");
        Response response = get("");
        return List.of(getOkAs(response, Order[].class));
    }

    /**
     * Gets orders with filtering.
     */
    public List<Order> getOrders(Map<String, Object> filters) {
        log.info("Getting orders with filters: {}", filters);
        Response response = get("", filters);
        return List.of(getOkAs(response, Order[].class));
    }

    /**
     * Gets order by order number.
     */
    public Order getOrderByNumber(String orderNumber) {
        log.info("Getting order by number: {}", orderNumber);
        Response response = get("/by-number/" + orderNumber);
        return getOkAs(response, Order.class);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ORDER OPERATIONS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Cancels an order.
     */
    public Order cancelOrder(String orderId) {
        log.info("Cancelling order: {}", orderId);
        Response response = post("/" + orderId + "/cancel");
        return getOkAs(response, Order.class);
    }

    /**
     * Cancels an order with reason.
     */
    public Order cancelOrder(String orderId, String reason) {
        log.info("Cancelling order: {} with reason: {}", orderId, reason);
        Response response = post("/" + orderId + "/cancel", Map.of(
            "reason", reason
        ));
        return getOkAs(response, Order.class);
    }

    /**
     * Requests a refund for an order.
     */
    public RefundResponse requestRefund(String orderId) {
        log.info("Requesting refund for order: {}", orderId);
        Response response = post("/" + orderId + "/refund");
        return getOkAs(response, RefundResponse.class);
    }

    /**
     * Requests a partial refund for specific items.
     */
    public RefundResponse requestPartialRefund(String orderId, List<String> itemIds) {
        log.info("Requesting partial refund for order: {}, items: {}", orderId, itemIds);
        Response response = post("/" + orderId + "/refund", Map.of(
            "itemIds", itemIds
        ));
        return getOkAs(response, RefundResponse.class);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // STATUS CHECKS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Waits for order to reach a specific status.
     */
    public Order waitForStatus(String orderId, OrderStatus expectedStatus, int maxWaitSeconds) {
        log.info("Waiting for order {} to reach status: {}", orderId, expectedStatus);

        long startTime = System.currentTimeMillis();
        long maxWaitMs = maxWaitSeconds * 1000L;

        while (System.currentTimeMillis() - startTime < maxWaitMs) {
            Order order = getOrder(orderId);
            if (order.getStatus() == expectedStatus) {
                log.info("Order reached status: {}", expectedStatus);
                return order;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while waiting for order status", e);
            }
        }

        throw new RuntimeException(String.format(
            "Order %s did not reach status %s within %d seconds",
            orderId, expectedStatus, maxWaitSeconds));
    }

    /**
     * Checks if order is confirmed.
     */
    public boolean isOrderConfirmed(String orderId) {
        Order order = getOrder(orderId);
        return order.isConfirmed() && order.isPaid();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // HELPER CLASSES
    // ═══════════════════════════════════════════════════════════════════════════

    public static class RefundResponse {
        public String refundId;
        public String status;
        public java.math.BigDecimal amount;
        public String message;
    }
}
