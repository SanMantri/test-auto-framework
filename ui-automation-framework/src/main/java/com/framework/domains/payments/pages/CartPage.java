package com.framework.domains.payments.pages;

import com.framework.core.base.BasePage;
import com.microsoft.playwright.Page;
import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.List;

/**
 * CartPage - Shopping cart page object
 *
 * Handles all cart-related UI interactions including:
 * - Viewing cart items
 * - Updating quantities
 * - Removing items
 * - Applying coupons
 * - Proceeding to checkout
 */
@Slf4j
public class CartPage extends BasePage {

    // ═══════════════════════════════════════════════════════════════════════════
    // LOCATORS
    // ═══════════════════════════════════════════════════════════════════════════

    // Cart Container
    private static final String CART_CONTAINER = "[data-testid='cart-container'], .cart-container";
    private static final String EMPTY_CART_MESSAGE = "[data-testid='empty-cart'], .empty-cart-message";

    // Cart Items
    private static final String CART_ITEM = "[data-testid='cart-item']";
    private static final String CART_ITEM_BY_ID = "[data-testid='cart-item-%s']";
    private static final String ITEM_NAME = "[data-testid='item-name'], .item-name";
    private static final String ITEM_PRICE = "[data-testid='item-price'], .item-price";
    private static final String ITEM_QUANTITY = "[data-testid='item-quantity'], .item-quantity";
    private static final String ITEM_TOTAL = "[data-testid='item-total'], .item-total";

    // Quantity Controls
    private static final String QUANTITY_INPUT = "[data-testid='quantity-input-%s'], input[name='quantity-%s']";
    private static final String QUANTITY_INCREASE = "[data-testid='qty-increase-%s'], button:has-text('+')";
    private static final String QUANTITY_DECREASE = "[data-testid='qty-decrease-%s'], button:has-text('-')";
    private static final String REMOVE_ITEM = "[data-testid='remove-item-%s'], button:has-text('Remove')";

    // Coupon
    private static final String COUPON_INPUT = "[data-testid='coupon-input'], #coupon-code, input[name='coupon']";
    private static final String APPLY_COUPON_BUTTON = "[data-testid='apply-coupon'], button:has-text('Apply')";
    private static final String REMOVE_COUPON_BUTTON = "[data-testid='remove-coupon']";
    private static final String COUPON_SUCCESS = "[data-testid='coupon-success'], .coupon-applied";
    private static final String COUPON_ERROR = "[data-testid='coupon-error'], .coupon-error";
    private static final String DISCOUNT_AMOUNT = "[data-testid='discount-amount'], .discount-amount";

    // Totals
    private static final String SUBTOTAL = "[data-testid='cart-subtotal'], .cart-subtotal";
    private static final String TAX_AMOUNT = "[data-testid='tax-amount'], .tax-amount";
    private static final String SHIPPING_COST = "[data-testid='shipping-cost'], .shipping-cost";
    private static final String TOTAL_AMOUNT = "[data-testid='cart-total'], .cart-total";

    // Actions
    private static final String CHECKOUT_BUTTON = "[data-testid='checkout-btn'], button:has-text('Checkout'), #checkout";
    private static final String CONTINUE_SHOPPING = "[data-testid='continue-shopping'], a:has-text('Continue Shopping')";
    private static final String CLEAR_CART_BUTTON = "[data-testid='clear-cart']";

    // Loading States
    private static final String CART_LOADING = "[data-testid='cart-loading'], .cart-loading";
    private static final String UPDATING_INDICATOR = "[data-testid='updating'], .updating";

    // ═══════════════════════════════════════════════════════════════════════════
    // CONSTRUCTOR & NAVIGATION
    // ═══════════════════════════════════════════════════════════════════════════

    public CartPage(Page page) {
        super(page);
    }

    @Step("Navigate to cart page")
    public CartPage navigate() {
        navigateTo("/cart");
        return this;
    }

    @Override
    protected void waitForPageLoad() {
        waitForHidden(CART_LOADING);
        waitForVisible(CART_CONTAINER);
    }

    @Override
    public boolean isDisplayed() {
        return isVisible(CART_CONTAINER);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CART STATUS
    // ═══════════════════════════════════════════════════════════════════════════

    public boolean isEmpty() {
        return isVisible(EMPTY_CART_MESSAGE);
    }

    public int getItemCount() {
        if (isEmpty()) {
            return 0;
        }
        return page.locator(CART_ITEM).count();
    }

    public List<String> getCartItemIds() {
        return page.locator(CART_ITEM)
            .all()
            .stream()
            .map(el -> el.getAttribute("data-item-id"))
            .toList();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ITEM OPERATIONS
    // ═══════════════════════════════════════════════════════════════════════════

    @Step("Get item name for item: {itemId}")
    public String getItemName(String itemId) {
        String locator = String.format(CART_ITEM_BY_ID, itemId) + " " + ITEM_NAME;
        return getText(locator);
    }

    @Step("Get item price for item: {itemId}")
    public String getItemPrice(String itemId) {
        String locator = String.format(CART_ITEM_BY_ID, itemId) + " " + ITEM_PRICE;
        return getText(locator);
    }

    @Step("Get item quantity for item: {itemId}")
    public int getItemQuantity(String itemId) {
        String locator = String.format(QUANTITY_INPUT, itemId, itemId);
        String value = getAttribute(locator, "value");
        return Integer.parseInt(value);
    }

    @Step("Update quantity to {quantity} for item: {itemId}")
    public CartPage updateQuantity(String itemId, int quantity) {
        log.info("Updating quantity to {} for item: {}", quantity, itemId);

        String inputLocator = String.format(QUANTITY_INPUT, itemId, itemId);
        fill(inputLocator, String.valueOf(quantity));

        // Wait for cart to update
        waitForHidden(UPDATING_INDICATOR);
        waitFor(500); // Allow recalculation

        return this;
    }

    @Step("Increase quantity for item: {itemId}")
    public CartPage increaseQuantity(String itemId) {
        log.info("Increasing quantity for item: {}", itemId);

        String buttonLocator = String.format(QUANTITY_INCREASE, itemId);
        click(buttonLocator);

        waitForHidden(UPDATING_INDICATOR);
        return this;
    }

    @Step("Decrease quantity for item: {itemId}")
    public CartPage decreaseQuantity(String itemId) {
        log.info("Decreasing quantity for item: {}", itemId);

        String buttonLocator = String.format(QUANTITY_DECREASE, itemId);
        click(buttonLocator);

        waitForHidden(UPDATING_INDICATOR);
        return this;
    }

    @Step("Remove item from cart: {itemId}")
    public CartPage removeItem(String itemId) {
        log.info("Removing item from cart: {}", itemId);

        String buttonLocator = String.format(REMOVE_ITEM, itemId);
        click(buttonLocator);

        // Wait for item to be removed
        String itemLocator = String.format(CART_ITEM_BY_ID, itemId);
        waitForHidden(itemLocator);

        return this;
    }

    @Step("Clear entire cart")
    public CartPage clearCart() {
        log.info("Clearing entire cart");

        if (!isEmpty()) {
            click(CLEAR_CART_BUTTON);
            waitForVisible(EMPTY_CART_MESSAGE);
        }

        return this;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // COUPON OPERATIONS
    // ═══════════════════════════════════════════════════════════════════════════

    @Step("Apply coupon code: {couponCode}")
    public CartPage applyCoupon(String couponCode) {
        log.info("Applying coupon: {}", couponCode);

        fill(COUPON_INPUT, couponCode);
        click(APPLY_COUPON_BUTTON);

        // Wait for coupon to be applied
        waitFor(1000);

        return this;
    }

    public boolean isCouponApplied() {
        return isVisible(COUPON_SUCCESS);
    }

    public boolean hasCouponError() {
        return isVisible(COUPON_ERROR);
    }

    public String getCouponErrorMessage() {
        if (hasCouponError()) {
            return getText(COUPON_ERROR);
        }
        return null;
    }

    @Step("Remove applied coupon")
    public CartPage removeCoupon() {
        log.info("Removing coupon");

        if (isCouponApplied()) {
            click(REMOVE_COUPON_BUTTON);
            waitForHidden(COUPON_SUCCESS);
        }

        return this;
    }

    public String getDiscountAmount() {
        if (isCouponApplied()) {
            return getText(DISCOUNT_AMOUNT);
        }
        return null;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TOTALS
    // ═══════════════════════════════════════════════════════════════════════════

    public String getSubtotal() {
        return getText(SUBTOTAL);
    }

    public String getTaxAmount() {
        return getText(TAX_AMOUNT);
    }

    public String getShippingCost() {
        return getText(SHIPPING_COST);
    }

    public String getTotalAmount() {
        return getText(TOTAL_AMOUNT);
    }

    public BigDecimal getTotalAsDecimal() {
        String total = getTotalAmount()
            .replaceAll("[^0-9.]", ""); // Remove currency symbols
        return new BigDecimal(total);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // NAVIGATION ACTIONS
    // ═══════════════════════════════════════════════════════════════════════════

    @Step("Proceed to checkout")
    public CheckoutPage proceedToCheckout() {
        log.info("Proceeding to checkout");

        click(CHECKOUT_BUTTON);
        waitForUrl(".*/checkout.*");

        return new CheckoutPage(page);
    }

    @Step("Continue shopping")
    public void continueShopping() {
        log.info("Continuing shopping");
        click(CONTINUE_SHOPPING);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // VALIDATION HELPERS
    // ═══════════════════════════════════════════════════════════════════════════

    public boolean hasItem(String itemId) {
        String locator = String.format(CART_ITEM_BY_ID, itemId);
        return isVisible(locator);
    }

    public boolean isCheckoutEnabled() {
        return page.locator(CHECKOUT_BUTTON).isEnabled();
    }
}
