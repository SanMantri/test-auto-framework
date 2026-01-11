package com.framework.domains.payments.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Cart - Shopping cart data model
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Cart {

    private String id;
    private String userId;
    private List<CartItem> items;
    private BigDecimal subtotal;
    private BigDecimal tax;
    private BigDecimal shipping;
    private BigDecimal discount;
    private BigDecimal total;
    private String couponCode;
    private String currency;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CartItem {
        private String id;
        private String productId;
        private String name;
        private String description;
        private int quantity;
        private BigDecimal price;
        private BigDecimal totalPrice;
        private String imageUrl;
    }

    // Convenience methods
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    public int getTotalQuantity() {
        return items != null
            ? items.stream().mapToInt(CartItem::getQuantity).sum()
            : 0;
    }

    public boolean isEmpty() {
        return items == null || items.isEmpty();
    }

    public boolean hasCoupon() {
        return couponCode != null && !couponCode.isEmpty();
    }
}
