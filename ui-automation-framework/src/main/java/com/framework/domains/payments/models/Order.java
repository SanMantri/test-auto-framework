package com.framework.domains.payments.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Order - Order data model
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    private String id;
    private String orderNumber;
    private String userId;
    private OrderStatus status;
    private PaymentStatus paymentStatus;
    private List<OrderItem> items;
    private Address shippingAddress;
    private Address billingAddress;
    private BigDecimal subtotal;
    private BigDecimal tax;
    private BigDecimal shipping;
    private BigDecimal discount;
    private BigDecimal total;
    private String currency;
    private PaymentInfo payment;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String trackingNumber;
    private LocalDateTime estimatedDelivery;

    public enum OrderStatus {
        PENDING,
        CONFIRMED,
        PROCESSING,
        SHIPPED,
        DELIVERED,
        CANCELLED,
        REFUNDED
    }

    public enum PaymentStatus {
        PENDING,
        AUTHORIZED,
        CAPTURED,
        FAILED,
        REFUNDED,
        PARTIALLY_REFUNDED
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItem {
        private String id;
        private String productId;
        private String name;
        private int quantity;
        private BigDecimal price;
        private BigDecimal totalPrice;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Address {
        private String id;
        private String name;
        private String line1;
        private String line2;
        private String city;
        private String state;
        private String postalCode;
        private String country;
        private String phone;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentInfo {
        private String method;
        private String last4;
        private String brand;
        private String transactionId;
    }

    // Convenience methods
    public boolean isConfirmed() {
        return status == OrderStatus.CONFIRMED;
    }

    public boolean isPaid() {
        return paymentStatus == PaymentStatus.CAPTURED;
    }

    public boolean isDelivered() {
        return status == OrderStatus.DELIVERED;
    }

    public boolean isCancelled() {
        return status == OrderStatus.CANCELLED || status == OrderStatus.REFUNDED;
    }
}
