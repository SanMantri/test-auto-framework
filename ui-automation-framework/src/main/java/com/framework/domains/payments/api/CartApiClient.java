package com.framework.domains.payments.api;

import com.framework.core.base.BaseApiClient;
import com.framework.domains.payments.models.Cart;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * CartApiClient - API operations for shopping cart
 *
 * Handles cart CRUD operations via API.
 * Used for fast test setup instead of UI operations.
 */
@Slf4j
@Component
public class CartApiClient extends BaseApiClient {

    @Override
    protected String getBasePath() {
        return "/api/v1/cart";
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CART OPERATIONS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Creates a new cart for the authenticated user.
     */
    public Cart createCart() {
        log.info("Creating new cart");
        Response response = post("");
        return getCreatedAs(response, Cart.class);
    }

    /**
     * Gets the current user's cart.
     */
    public Cart getCart() {
        log.info("Getting cart");
        Response response = get("");
        return getOkAs(response, Cart.class);
    }

    /**
     * Gets a cart by ID.
     */
    public Cart getCart(String cartId) {
        log.info("Getting cart: {}", cartId);
        Response response = get("/" + cartId);
        return getOkAs(response, Cart.class);
    }

    /**
     * Clears all items from cart.
     */
    public Cart clearCart() {
        log.info("Clearing cart");
        Response response = delete("/items");
        return getOkAs(response, Cart.class);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ITEM OPERATIONS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Adds an item to cart.
     */
    public Cart addItem(String productId, int quantity) {
        log.info("Adding item to cart: productId={}, qty={}", productId, quantity);
        Response response = post("/items", Map.of(
            "productId", productId,
            "quantity", quantity
        ));
        return getOkAs(response, Cart.class);
    }

    /**
     * Adds multiple items to cart.
     */
    public Cart addItems(List<Map<String, Object>> items) {
        log.info("Adding {} items to cart", items.size());
        Response response = post("/items/bulk", Map.of("items", items));
        return getOkAs(response, Cart.class);
    }

    /**
     * Updates item quantity in cart.
     */
    public Cart updateItemQuantity(String itemId, int quantity) {
        log.info("Updating item quantity: itemId={}, qty={}", itemId, quantity);
        Response response = patch("/items/" + itemId, Map.of(
            "quantity", quantity
        ));
        return getOkAs(response, Cart.class);
    }

    /**
     * Removes an item from cart.
     */
    public Cart removeItem(String itemId) {
        log.info("Removing item from cart: {}", itemId);
        Response response = delete("/items/" + itemId);
        return getOkAs(response, Cart.class);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // COUPON OPERATIONS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Applies a coupon code to cart.
     */
    public Cart applyCoupon(String couponCode) {
        log.info("Applying coupon: {}", couponCode);
        Response response = post("/coupon", Map.of(
            "code", couponCode
        ));
        return getOkAs(response, Cart.class);
    }

    /**
     * Removes coupon from cart.
     */
    public Cart removeCoupon() {
        log.info("Removing coupon");
        Response response = delete("/coupon");
        return getOkAs(response, Cart.class);
    }

    /**
     * Validates if coupon is applicable.
     */
    public CouponValidation validateCoupon(String couponCode) {
        log.info("Validating coupon: {}", couponCode);
        Response response = get("/coupon/validate", Map.of("code", couponCode));
        return getOkAs(response, CouponValidation.class);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // HELPER CLASSES
    // ═══════════════════════════════════════════════════════════════════════════

    public static class CouponValidation {
        public boolean valid;
        public String message;
        public java.math.BigDecimal discount;

        public boolean isValid() {
            return valid;
        }
    }
}
