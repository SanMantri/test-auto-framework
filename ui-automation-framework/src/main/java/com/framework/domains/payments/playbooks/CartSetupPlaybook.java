package com.framework.domains.payments.playbooks;

import com.framework.core.data.GlobalDataCache;
import com.framework.core.data.TestDataCache;
import com.framework.domains.payments.api.CartApiClient;
import com.framework.domains.payments.models.Cart;
import io.qameta.allure.Step;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * CartSetupPlaybook - Reusable cart setup via API
 *
 * This playbook provides fast cart setup using API calls instead of UI.
 * It's designed to be imported into tests that need a cart with specific items.
 *
 * Usage:
 *   cartSetupPlaybook.setupCartWithItems(List.of("PROD-001", "PROD-002"))
 *   cartSetupPlaybook.setupCartWithCoupon("PROD-001", "SAVE10")
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CartSetupPlaybook {

    private final CartApiClient cartApi;
    private final GlobalDataCache globalDataCache;

    // ═══════════════════════════════════════════════════════════════════════════
    // SIMPLE CART SETUP
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Sets up a cart with a single item.
     *
     * @param productId Product ID to add
     * @return Created cart
     */
    @Step("Setup cart with single product: {productId}")
    public Cart setupCartWithSingleItem(String productId) {
        return setupCartWithSingleItem(productId, 1);
    }

    /**
     * Sets up a cart with a single item and specified quantity.
     *
     * @param productId Product ID to add
     * @param quantity  Quantity to add
     * @return Created cart
     */
    @Step("Setup cart with product: {productId}, quantity: {quantity}")
    public Cart setupCartWithSingleItem(String productId, int quantity) {
        log.info("Setting up cart with product: {}, qty: {}", productId, quantity);

        // Clear any existing cart
        try {
            cartApi.clearCart();
        } catch (Exception e) {
            // Cart might not exist yet
            log.debug("No existing cart to clear");
        }

        // Add item
        Cart cart = cartApi.addItem(productId, quantity);

        log.info("Cart created with {} item(s), total: {}", cart.getItemCount(), cart.getTotal());

        return cart;
    }

    /**
     * Sets up a cart with multiple items (quantity 1 each).
     *
     * @param productIds List of product IDs
     * @return Created cart
     */
    @Step("Setup cart with multiple products")
    public Cart setupCartWithItems(List<String> productIds) {
        log.info("Setting up cart with {} products", productIds.size());

        // Clear existing cart
        try {
            cartApi.clearCart();
        } catch (Exception e) {
            log.debug("No existing cart to clear");
        }

        // Build items list with default quantity
        List<Map<String, Object>> items = productIds.stream()
            .map(id -> Map.<String, Object>of("productId", id, "quantity", 1))
            .toList();

        // Add all items
        Cart cart = cartApi.addItems(items);

        log.info("Cart created with {} item(s), total: {}", cart.getItemCount(), cart.getTotal());

        return cart;
    }

    /**
     * Sets up a cart with specific items and quantities.
     *
     * @param items List of maps with productId and quantity
     * @return Created cart
     */
    @Step("Setup cart with specified items and quantities")
    public Cart setupCartWithItemsAndQuantities(List<Map<String, Object>> items) {
        log.info("Setting up cart with {} items", items.size());

        // Clear existing cart
        try {
            cartApi.clearCart();
        } catch (Exception e) {
            log.debug("No existing cart to clear");
        }

        // Add items
        Cart cart = cartApi.addItems(items);

        log.info("Cart created with {} item(s), total: {}", cart.getItemCount(), cart.getTotal());

        return cart;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CART WITH COUPON
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Sets up a cart with a single item and applies a coupon.
     *
     * @param productId  Product ID to add
     * @param couponCode Coupon code to apply
     * @return Created cart with coupon applied
     */
    @Step("Setup cart with product and coupon")
    public Cart setupCartWithCoupon(String productId, String couponCode) {
        log.info("Setting up cart with product: {} and coupon: {}", productId, couponCode);

        Cart cart = setupCartWithSingleItem(productId);
        cart = cartApi.applyCoupon(couponCode);

        log.info("Cart total after coupon: {}, discount: {}", cart.getTotal(), cart.getDiscount());

        return cart;
    }

    /**
     * Sets up a cart with multiple items and applies a coupon.
     *
     * @param productIds List of product IDs
     * @param couponCode Coupon code to apply
     * @return Created cart with coupon applied
     */
    @Step("Setup cart with multiple products and coupon")
    public Cart setupCartWithItemsAndCoupon(List<String> productIds, String couponCode) {
        log.info("Setting up cart with {} products and coupon: {}", productIds.size(), couponCode);

        Cart cart = setupCartWithItems(productIds);
        cart = cartApi.applyCoupon(couponCode);

        log.info("Cart total after coupon: {}, discount: {}", cart.getTotal(), cart.getDiscount());

        return cart;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PRE-CONFIGURED SCENARIOS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Sets up a minimal cart for quick checkout testing.
     * Uses a cached product ID for fastest setup.
     *
     * @return Cart with single low-value item
     */
    @Step("Setup minimal cart for quick checkout")
    public Cart setupMinimalCart() {
        String productId = (String) globalDataCache.get("defaultProductId");
        if (productId == null) {
            productId = "PROD-DEFAULT-001";
        }

        return setupCartWithSingleItem(productId, 1);
    }

    /**
     * Sets up a cart that meets minimum order value for free shipping.
     *
     * @return Cart meeting free shipping threshold
     */
    @Step("Setup cart for free shipping")
    public Cart setupFreeShippingCart() {
        log.info("Setting up cart for free shipping threshold");

        // Get free shipping products from global cache or use defaults
        @SuppressWarnings("unchecked")
        List<String> freeShippingProducts = (List<String>) globalDataCache.get("freeShippingProducts");

        if (freeShippingProducts == null || freeShippingProducts.isEmpty()) {
            // Default products that together meet threshold
            freeShippingProducts = List.of("PROD-HIGH-001", "PROD-HIGH-002");
        }

        return setupCartWithItems(freeShippingProducts);
    }

    /**
     * Sets up a cart with high-value items for payment limit testing.
     *
     * @return High-value cart
     */
    @Step("Setup high-value cart")
    public Cart setupHighValueCart() {
        log.info("Setting up high-value cart");

        List<Map<String, Object>> items = List.of(
            Map.of("productId", "PROD-PREMIUM-001", "quantity", 2),
            Map.of("productId", "PROD-PREMIUM-002", "quantity", 1)
        );

        return setupCartWithItemsAndQuantities(items);
    }

    /**
     * Sets up a cart with mixed inventory (some in-stock, some low-stock).
     * Useful for testing inventory-related scenarios.
     *
     * @return Cart with mixed inventory items
     */
    @Step("Setup cart with mixed inventory items")
    public Cart setupMixedInventoryCart() {
        log.info("Setting up cart with mixed inventory items");

        List<String> products = List.of(
            "PROD-INSTOCK-001",    // High stock
            "PROD-LOWSTOCK-001",   // Low stock warning
            "PROD-LASTITEM-001"    // Only 1 left
        );

        return setupCartWithItems(products);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CART STATE MANAGEMENT
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Clears the current cart.
     *
     * @return Empty cart
     */
    @Step("Clear cart")
    public Cart clearCart() {
        log.info("Clearing cart");
        return cartApi.clearCart();
    }

    /**
     * Gets the current cart state.
     *
     * @return Current cart
     */
    @Step("Get current cart")
    public Cart getCart() {
        return cartApi.getCart();
    }

    /**
     * Stores cart in test data cache for later verification.
     *
     * @param cart      Cart to store
     * @param testData  TestDataCache instance
     */
    public void storeCartInTestData(Cart cart, TestDataCache testData) {
        testData.put("cart", cart);
        testData.put("cartId", cart.getId());
        testData.put("cartTotal", cart.getTotal());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // VALIDATION HELPERS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Validates coupon is applicable before applying.
     *
     * @param couponCode Coupon to validate
     * @return true if coupon is valid
     */
    public boolean isCouponValid(String couponCode) {
        CartApiClient.CouponValidation validation = cartApi.validateCoupon(couponCode);
        return validation.isValid();
    }
}
