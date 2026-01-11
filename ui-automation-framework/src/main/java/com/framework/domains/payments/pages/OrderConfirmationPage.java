package com.framework.domains.payments.pages;

import com.framework.core.base.BasePage;
import com.microsoft.playwright.Page;
import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;

/**
 * OrderConfirmationPage - Order confirmation page after successful payment
 *
 * Displays:
 * - Order number and confirmation message
 * - Order details summary
 * - Estimated delivery
 * - Navigation options
 */
@Slf4j
public class OrderConfirmationPage extends BasePage {

    // ═══════════════════════════════════════════════════════════════════════════
    // LOCATORS
    // ═══════════════════════════════════════════════════════════════════════════

    // Success Indicator
    private static final String SUCCESS_ICON = "[data-testid='success-icon'], .success-checkmark";
    private static final String CONFIRMATION_MESSAGE = "[data-testid='confirmation-message'], .confirmation-title";

    // Order Details
    private static final String ORDER_NUMBER = "[data-testid='order-number'], .order-number";
    private static final String ORDER_DATE = "[data-testid='order-date'], .order-date";
    private static final String ORDER_STATUS = "[data-testid='order-status'], .order-status";

    // Delivery Information
    private static final String DELIVERY_ADDRESS = "[data-testid='delivery-address']";
    private static final String ESTIMATED_DELIVERY = "[data-testid='estimated-delivery'], .delivery-estimate";

    // Payment Information
    private static final String PAYMENT_METHOD_USED = "[data-testid='payment-method'], .payment-method";
    private static final String TRANSACTION_ID = "[data-testid='transaction-id'], .transaction-id";

    // Order Summary
    private static final String ORDER_ITEMS = "[data-testid='order-items']";
    private static final String ORDER_ITEM = "[data-testid='order-item']";
    private static final String ORDER_SUBTOTAL = "[data-testid='order-subtotal']";
    private static final String ORDER_SHIPPING = "[data-testid='order-shipping']";
    private static final String ORDER_TAX = "[data-testid='order-tax']";
    private static final String ORDER_DISCOUNT = "[data-testid='order-discount']";
    private static final String ORDER_TOTAL = "[data-testid='order-total']";

    // Actions
    private static final String VIEW_ORDER_BUTTON = "[data-testid='view-order'], a:has-text('View Order')";
    private static final String TRACK_ORDER_BUTTON = "[data-testid='track-order'], a:has-text('Track')";
    private static final String CONTINUE_SHOPPING_BUTTON = "[data-testid='continue-shopping'], a:has-text('Continue Shopping')";
    private static final String PRINT_RECEIPT_BUTTON = "[data-testid='print-receipt'], button:has-text('Print')";
    private static final String DOWNLOAD_INVOICE_BUTTON = "[data-testid='download-invoice']";

    // Email Confirmation
    private static final String EMAIL_CONFIRMATION = "[data-testid='email-sent'], .email-confirmation";

    // ═══════════════════════════════════════════════════════════════════════════
    // CONSTRUCTOR & NAVIGATION
    // ═══════════════════════════════════════════════════════════════════════════

    public OrderConfirmationPage(Page page) {
        super(page);
    }

    @Override
    protected void waitForPageLoad() {
        waitForVisible(SUCCESS_ICON);
        waitForVisible(ORDER_NUMBER);
    }

    @Override
    public boolean isDisplayed() {
        return isVisible(SUCCESS_ICON) && isVisible(ORDER_NUMBER);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ORDER INFORMATION
    // ═══════════════════════════════════════════════════════════════════════════

    @Step("Get order number")
    public String getOrderNumber() {
        String text = getText(ORDER_NUMBER);
        // Extract order number from text like "Order #ORD-123456"
        return text.replaceAll(".*#", "").trim();
    }

    public String getOrderDate() {
        return getText(ORDER_DATE);
    }

    public String getOrderStatus() {
        return getText(ORDER_STATUS);
    }

    public String getConfirmationMessage() {
        return getText(CONFIRMATION_MESSAGE);
    }

    public boolean isOrderConfirmed() {
        return isVisible(SUCCESS_ICON) &&
               getConfirmationMessage().toLowerCase().contains("confirmed");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DELIVERY INFORMATION
    // ═══════════════════════════════════════════════════════════════════════════

    public String getDeliveryAddress() {
        return getText(DELIVERY_ADDRESS);
    }

    public String getEstimatedDelivery() {
        return getText(ESTIMATED_DELIVERY);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PAYMENT INFORMATION
    // ═══════════════════════════════════════════════════════════════════════════

    public String getPaymentMethod() {
        return getText(PAYMENT_METHOD_USED);
    }

    public String getTransactionId() {
        if (isVisible(TRANSACTION_ID)) {
            return getText(TRANSACTION_ID);
        }
        return null;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ORDER SUMMARY
    // ═══════════════════════════════════════════════════════════════════════════

    public int getItemCount() {
        return page.locator(ORDER_ITEM).count();
    }

    public String getSubtotal() {
        return getText(ORDER_SUBTOTAL);
    }

    public String getShippingCost() {
        return getText(ORDER_SHIPPING);
    }

    public String getTaxAmount() {
        return getText(ORDER_TAX);
    }

    public String getDiscount() {
        if (isVisible(ORDER_DISCOUNT)) {
            return getText(ORDER_DISCOUNT);
        }
        return null;
    }

    public String getOrderTotal() {
        return getText(ORDER_TOTAL);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // EMAIL CONFIRMATION
    // ═══════════════════════════════════════════════════════════════════════════

    public boolean isEmailConfirmationDisplayed() {
        return isVisible(EMAIL_CONFIRMATION);
    }

    public String getEmailConfirmationText() {
        if (isEmailConfirmationDisplayed()) {
            return getText(EMAIL_CONFIRMATION);
        }
        return null;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ACTIONS
    // ═══════════════════════════════════════════════════════════════════════════

    @Step("Navigate to order details")
    public void viewOrderDetails() {
        log.info("Viewing order details");
        click(VIEW_ORDER_BUTTON);
    }

    @Step("Track order")
    public void trackOrder() {
        log.info("Tracking order");
        click(TRACK_ORDER_BUTTON);
    }

    @Step("Continue shopping")
    public void continueShopping() {
        log.info("Continuing shopping");
        click(CONTINUE_SHOPPING_BUTTON);
    }

    @Step("Print receipt")
    public void printReceipt() {
        log.info("Printing receipt");
        click(PRINT_RECEIPT_BUTTON);
    }

    @Step("Download invoice")
    public void downloadInvoice() {
        log.info("Downloading invoice");
        click(DOWNLOAD_INVOICE_BUTTON);
        // Wait for download to start
        waitFor(1000);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // VERIFICATION HELPERS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Verifies order confirmation page is complete with all expected elements
     */
    public boolean hasCompleteOrderDetails() {
        return isVisible(ORDER_NUMBER) &&
               isVisible(ORDER_TOTAL) &&
               isVisible(DELIVERY_ADDRESS) &&
               isVisible(PAYMENT_METHOD_USED);
    }

    /**
     * Gets all confirmation details as a summary string for logging
     */
    public String getConfirmationSummary() {
        return String.format(
            "Order: %s | Total: %s | Delivery: %s | Payment: %s",
            getOrderNumber(),
            getOrderTotal(),
            getEstimatedDelivery(),
            getPaymentMethod()
        );
    }
}
