package com.framework.domains.payments.pages;

import com.framework.core.base.BasePage;
import com.framework.domains.payments.models.Order.Address;
import com.microsoft.playwright.Page;
import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * CheckoutPage - Checkout flow page object
 *
 * Handles the multi-step checkout process:
 * - Address selection/entry
 * - Shipping method selection
 * - Order review
 * - Navigation to payment
 */
@Slf4j
public class CheckoutPage extends BasePage {

    // ═══════════════════════════════════════════════════════════════════════════
    // LOCATORS
    // ═══════════════════════════════════════════════════════════════════════════

    // Progress Indicator
    private static final String CHECKOUT_STEP = "[data-testid='checkout-step-%s']";
    private static final String ACTIVE_STEP = "[data-testid='checkout-step-active']";

    // Address Selection
    private static final String SAVED_ADDRESS = "[data-testid='saved-address']";
    private static final String SAVED_ADDRESS_BY_ID = "[data-testid='address-%s']";
    private static final String ADD_NEW_ADDRESS = "[data-testid='add-new-address'], button:has-text('Add New Address')";
    private static final String SELECTED_ADDRESS = "[data-testid='selected-address'], .address-selected";

    // Address Form
    private static final String ADDRESS_FORM = "[data-testid='address-form']";
    private static final String FULL_NAME_INPUT = "[data-testid='full-name'], #fullName, input[name='fullName']";
    private static final String PHONE_INPUT = "[data-testid='phone'], #phone, input[name='phone']";
    private static final String ADDRESS_LINE1 = "[data-testid='address-line1'], #addressLine1, input[name='addressLine1']";
    private static final String ADDRESS_LINE2 = "[data-testid='address-line2'], #addressLine2, input[name='addressLine2']";
    private static final String CITY_INPUT = "[data-testid='city'], #city, input[name='city']";
    private static final String STATE_INPUT = "[data-testid='state'], #state, input[name='state']";
    private static final String PINCODE_INPUT = "[data-testid='pincode'], #pincode, input[name='pincode']";
    private static final String COUNTRY_SELECT = "[data-testid='country'], #country, select[name='country']";
    private static final String ADDRESS_TYPE = "[data-testid='address-type-%s']";
    private static final String SAVE_ADDRESS_BUTTON = "[data-testid='save-address'], button:has-text('Save')";

    // Shipping Method
    private static final String SHIPPING_METHOD = "[data-testid='shipping-method']";
    private static final String SHIPPING_OPTION = "[data-testid='shipping-%s']";
    private static final String SHIPPING_PRICE = "[data-testid='shipping-price-%s']";
    private static final String ESTIMATED_DELIVERY = "[data-testid='delivery-date-%s']";
    private static final String SELECTED_SHIPPING = "[data-testid='shipping-selected']";

    // Order Summary
    private static final String ORDER_SUMMARY = "[data-testid='order-summary']";
    private static final String SUMMARY_ITEM = "[data-testid='summary-item']";
    private static final String SUMMARY_SUBTOTAL = "[data-testid='summary-subtotal']";
    private static final String SUMMARY_SHIPPING = "[data-testid='summary-shipping']";
    private static final String SUMMARY_TAX = "[data-testid='summary-tax']";
    private static final String SUMMARY_DISCOUNT = "[data-testid='summary-discount']";
    private static final String SUMMARY_TOTAL = "[data-testid='summary-total']";

    // Action Buttons
    private static final String CONTINUE_BUTTON = "[data-testid='continue-btn'], button:has-text('Continue')";
    private static final String BACK_BUTTON = "[data-testid='back-btn'], button:has-text('Back')";
    private static final String PLACE_ORDER_BUTTON = "[data-testid='place-order'], button:has-text('Place Order')";

    // Validation
    private static final String FIELD_ERROR = "[data-testid='error-%s'], .field-error";
    private static final String ADDRESS_VALIDATION_ERROR = "[data-testid='address-error']";

    // Loading
    private static final String CHECKOUT_LOADING = "[data-testid='checkout-loading']";

    // ═══════════════════════════════════════════════════════════════════════════
    // CONSTRUCTOR & NAVIGATION
    // ═══════════════════════════════════════════════════════════════════════════

    public CheckoutPage(Page page) {
        super(page);
    }

    @Step("Navigate to checkout page")
    public CheckoutPage navigate() {
        navigateTo("/checkout");
        return this;
    }

    @Override
    protected void waitForPageLoad() {
        waitForHidden(CHECKOUT_LOADING);
        waitForVisible(ORDER_SUMMARY);
    }

    @Override
    public boolean isDisplayed() {
        return isVisible(ORDER_SUMMARY);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CHECKOUT STEPS
    // ═══════════════════════════════════════════════════════════════════════════

    public enum CheckoutStep {
        ADDRESS("address"),
        SHIPPING("shipping"),
        REVIEW("review"),
        PAYMENT("payment");

        private final String value;

        CheckoutStep(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public String getCurrentStep() {
        return getAttribute(ACTIVE_STEP, "data-step");
    }

    public boolean isOnStep(CheckoutStep step) {
        return step.getValue().equals(getCurrentStep());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ADDRESS SELECTION
    // ═══════════════════════════════════════════════════════════════════════════

    @Step("Select saved address: {addressId}")
    public CheckoutPage selectSavedAddress(String addressId) {
        log.info("Selecting saved address: {}", addressId);

        String locator = String.format(SAVED_ADDRESS_BY_ID, addressId);
        click(locator);
        waitForVisible(SELECTED_ADDRESS);

        return this;
    }

    public List<String> getSavedAddressIds() {
        return page.locator(SAVED_ADDRESS)
            .all()
            .stream()
            .map(el -> el.getAttribute("data-address-id"))
            .toList();
    }

    public int getSavedAddressCount() {
        return page.locator(SAVED_ADDRESS).count();
    }

    @Step("Click add new address")
    public CheckoutPage clickAddNewAddress() {
        log.info("Adding new address");
        click(ADD_NEW_ADDRESS);
        waitForVisible(ADDRESS_FORM);
        return this;
    }

    @Step("Enter new address")
    public CheckoutPage enterAddress(Address address) {
        log.info("Entering address: {}", address.getCity());

        fill(FULL_NAME_INPUT, address.getFullName());
        fill(PHONE_INPUT, address.getPhone());
        fill(ADDRESS_LINE1, address.getLine1());

        if (address.getLine2() != null) {
            fill(ADDRESS_LINE2, address.getLine2());
        }

        fill(CITY_INPUT, address.getCity());
        fill(STATE_INPUT, address.getState());
        fill(PINCODE_INPUT, address.getPincode());
        selectByValue(COUNTRY_SELECT, address.getCountry());

        if (address.getType() != null) {
            click(String.format(ADDRESS_TYPE, address.getType().toLowerCase()));
        }

        return this;
    }

    @Step("Save entered address")
    public CheckoutPage saveAddress() {
        click(SAVE_ADDRESS_BUTTON);
        waitForHidden(ADDRESS_FORM);
        return this;
    }

    @Step("Enter and save new address")
    public CheckoutPage addNewAddress(Address address) {
        clickAddNewAddress();
        enterAddress(address);
        saveAddress();
        return this;
    }

    public boolean hasAddressValidationError() {
        return isVisible(ADDRESS_VALIDATION_ERROR);
    }

    public String getFieldError(String fieldName) {
        String locator = String.format(FIELD_ERROR, fieldName);
        if (isVisible(locator)) {
            return getText(locator);
        }
        return null;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SHIPPING METHOD SELECTION
    // ═══════════════════════════════════════════════════════════════════════════

    public enum ShippingMethod {
        STANDARD("standard"),
        EXPRESS("express"),
        OVERNIGHT("overnight"),
        FREE("free");

        private final String value;

        ShippingMethod(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    @Step("Select shipping method: {method}")
    public CheckoutPage selectShippingMethod(ShippingMethod method) {
        log.info("Selecting shipping method: {}", method);

        String locator = String.format(SHIPPING_OPTION, method.getValue());
        click(locator);
        waitForVisible(SELECTED_SHIPPING);

        return this;
    }

    public String getShippingPrice(ShippingMethod method) {
        String locator = String.format(SHIPPING_PRICE, method.getValue());
        return getText(locator);
    }

    public String getEstimatedDelivery(ShippingMethod method) {
        String locator = String.format(ESTIMATED_DELIVERY, method.getValue());
        return getText(locator);
    }

    public boolean isShippingMethodAvailable(ShippingMethod method) {
        String locator = String.format(SHIPPING_OPTION, method.getValue());
        return isVisible(locator);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ORDER SUMMARY
    // ═══════════════════════════════════════════════════════════════════════════

    public int getSummaryItemCount() {
        return page.locator(SUMMARY_ITEM).count();
    }

    public String getSubtotal() {
        return getText(SUMMARY_SUBTOTAL);
    }

    public String getShippingCost() {
        return getText(SUMMARY_SHIPPING);
    }

    public String getTaxAmount() {
        return getText(SUMMARY_TAX);
    }

    public String getDiscount() {
        if (isVisible(SUMMARY_DISCOUNT)) {
            return getText(SUMMARY_DISCOUNT);
        }
        return null;
    }

    public String getOrderTotal() {
        return getText(SUMMARY_TOTAL);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // NAVIGATION ACTIONS
    // ═══════════════════════════════════════════════════════════════════════════

    @Step("Continue to next step")
    public CheckoutPage continueToNextStep() {
        log.info("Continuing to next step");
        click(CONTINUE_BUTTON);
        waitFor(500); // Allow step transition
        return this;
    }

    @Step("Go back to previous step")
    public CheckoutPage goBack() {
        log.info("Going back to previous step");
        click(BACK_BUTTON);
        waitFor(500);
        return this;
    }

    @Step("Proceed to payment")
    public PaymentPage proceedToPayment() {
        log.info("Proceeding to payment");

        click(CONTINUE_BUTTON);
        waitForUrl(".*/checkout/payment.*");

        return new PaymentPage(page);
    }

    @Step("Place order (for COD)")
    public OrderConfirmationPage placeOrder() {
        log.info("Placing order");

        click(PLACE_ORDER_BUTTON);
        waitForUrl(".*/order/confirmation.*");

        return new OrderConfirmationPage(page);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // COMPLETE CHECKOUT FLOW
    // ═══════════════════════════════════════════════════════════════════════════

    @Step("Complete checkout with saved address")
    public PaymentPage completeCheckoutWithSavedAddress(String addressId, ShippingMethod shipping) {
        log.info("Completing checkout with saved address: {}", addressId);

        // Address step
        selectSavedAddress(addressId);
        continueToNextStep();

        // Shipping step
        selectShippingMethod(shipping);
        continueToNextStep();

        // Review step - proceed to payment
        return proceedToPayment();
    }

    @Step("Complete checkout with new address")
    public PaymentPage completeCheckoutWithNewAddress(Address address, ShippingMethod shipping) {
        log.info("Completing checkout with new address");

        // Address step
        addNewAddress(address);
        continueToNextStep();

        // Shipping step
        selectShippingMethod(shipping);
        continueToNextStep();

        // Review step - proceed to payment
        return proceedToPayment();
    }
}
