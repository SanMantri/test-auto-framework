package com.framework.domains.payments.pages;

import com.framework.core.base.BasePage;
import com.framework.domains.payments.models.PaymentMethod;
import com.framework.domains.payments.models.TestCard;
import com.microsoft.playwright.FrameLocator;
import com.microsoft.playwright.Page;
import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;

/**
 * PaymentPage - Payment entry page object
 *
 * Handles all payment-related UI interactions including:
 * - Payment method selection
 * - Card details entry
 * - 3DS authentication
 * - UPI payments
 * - Payment submission
 */
@Slf4j
public class PaymentPage extends BasePage {

    // ═══════════════════════════════════════════════════════════════════════════
    // LOCATORS
    // ═══════════════════════════════════════════════════════════════════════════

    // Payment Method Selection
    private static final String PAYMENT_METHOD_RADIO = "[data-testid='payment-method-%s']";
    private static final String PAYMENT_METHOD_ACTIVE = "[data-testid='payment-method-active']";

    // Card Payment
    private static final String CARD_NUMBER_INPUT = "[data-testid='card-number'], #card-number, input[name='cardNumber']";
    private static final String CARD_EXPIRY_INPUT = "[data-testid='card-expiry'], #card-expiry, input[name='expiry']";
    private static final String CARD_CVV_INPUT = "[data-testid='card-cvv'], #card-cvv, input[name='cvv']";
    private static final String CARD_NAME_INPUT = "[data-testid='card-name'], #card-name, input[name='cardName']";
    private static final String CARD_IFRAME = "iframe[name='card-frame'], iframe[data-testid='card-iframe']";

    // UPI Payment
    private static final String UPI_ID_INPUT = "[data-testid='upi-id'], #upi-id, input[name='upiId']";
    private static final String UPI_VERIFY_BUTTON = "[data-testid='verify-upi'], button:has-text('Verify')";
    private static final String UPI_VERIFIED_BADGE = "[data-testid='upi-verified'], .upi-verified";

    // Net Banking
    private static final String BANK_SELECT = "[data-testid='bank-select'], #bank-select";
    private static final String BANK_OPTION = "[data-testid='bank-%s']";

    // Action Buttons
    private static final String PAY_NOW_BUTTON = "[data-testid='pay-now-btn'], button:has-text('Pay'), #pay-button";
    private static final String CANCEL_BUTTON = "[data-testid='cancel-payment']";

    // 3DS Authentication
    private static final String THREE_DS_IFRAME = "iframe[name='three-ds-challenge'], iframe[data-testid='3ds-frame']";
    private static final String THREE_DS_OTP_INPUT = "#otp-input, input[name='otp'], [data-testid='otp-input']";
    private static final String THREE_DS_SUBMIT = "#submit-otp, button:has-text('Submit'), [data-testid='submit-otp']";

    // Status
    private static final String PAYMENT_PROCESSING = "[data-testid='payment-processing'], .payment-processing";
    private static final String PAYMENT_ERROR = "[data-testid='payment-error'], .payment-error";
    private static final String PAYMENT_SUCCESS = "[data-testid='payment-success'], .payment-success";

    // Order Summary
    private static final String ORDER_TOTAL = "[data-testid='order-total'], .order-total";

    // ═══════════════════════════════════════════════════════════════════════════
    // CONSTRUCTOR & NAVIGATION
    // ═══════════════════════════════════════════════════════════════════════════

    public PaymentPage(Page page) {
        super(page);
    }

    @Step("Navigate to payment page")
    public PaymentPage navigate() {
        navigateTo("/checkout/payment");
        return this;
    }

    @Override
    protected void waitForPageLoad() {
        waitForVisible(PAY_NOW_BUTTON);
    }

    @Override
    public boolean isDisplayed() {
        return isVisible(PAY_NOW_BUTTON);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PAYMENT METHOD SELECTION
    // ═══════════════════════════════════════════════════════════════════════════

    @Step("Select payment method: {method}")
    public PaymentPage selectPaymentMethod(PaymentMethod method) {
        log.info("Selecting payment method: {}", method);

        String locator = String.format(PAYMENT_METHOD_RADIO, method.getValue());
        click(locator);

        // Wait for form to load based on method
        switch (method) {
            case CREDIT_CARD, DEBIT_CARD -> waitForVisible(CARD_NUMBER_INPUT, SHORT_TIMEOUT);
            case UPI -> waitForVisible(UPI_ID_INPUT, SHORT_TIMEOUT);
            case NET_BANKING -> waitForVisible(BANK_SELECT, SHORT_TIMEOUT);
        }

        return this;
    }

    public String getSelectedPaymentMethod() {
        return getAttribute(PAYMENT_METHOD_ACTIVE, "data-method");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CARD PAYMENT
    // ═══════════════════════════════════════════════════════════════════════════

    @Step("Enter card number")
    public PaymentPage enterCardNumber(String cardNumber) {
        log.debug("Entering card number");

        // Check if card input is in an iframe (some payment providers)
        if (isVisible(CARD_IFRAME)) {
            FrameLocator frame = frameLocator(CARD_IFRAME);
            frame.locator(CARD_NUMBER_INPUT).fill(cardNumber);
        } else {
            fill(CARD_NUMBER_INPUT, cardNumber);
        }

        return this;
    }

    @Step("Enter card expiry")
    public PaymentPage enterExpiry(String expiry) {
        log.debug("Entering card expiry: {}", expiry);
        fill(CARD_EXPIRY_INPUT, expiry);
        return this;
    }

    @Step("Enter card CVV")
    public PaymentPage enterCVV(String cvv) {
        log.debug("Entering card CVV");
        fill(CARD_CVV_INPUT, cvv);
        return this;
    }

    @Step("Enter cardholder name")
    public PaymentPage enterCardName(String name) {
        log.debug("Entering cardholder name: {}", name);
        fill(CARD_NAME_INPUT, name);
        return this;
    }

    @Step("Enter card details")
    public PaymentPage enterCardDetails(TestCard card) {
        log.info("Entering card details for test card");
        enterCardNumber(card.getNumber());
        enterExpiry(card.getExpiry());
        enterCVV(card.getCvv());
        if (card.getName() != null && isVisible(CARD_NAME_INPUT)) {
            enterCardName(card.getName());
        }
        return this;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // UPI PAYMENT
    // ═══════════════════════════════════════════════════════════════════════════

    @Step("Enter UPI ID: {upiId}")
    public PaymentPage enterUPIId(String upiId) {
        log.info("Entering UPI ID: {}", upiId);
        fill(UPI_ID_INPUT, upiId);
        return this;
    }

    @Step("Verify UPI ID")
    public PaymentPage verifyUPIId() {
        log.info("Verifying UPI ID");
        click(UPI_VERIFY_BUTTON);
        waitForVisible(UPI_VERIFIED_BADGE);
        return this;
    }

    public boolean isUPIVerified() {
        return isVisible(UPI_VERIFIED_BADGE);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // NET BANKING
    // ═══════════════════════════════════════════════════════════════════════════

    @Step("Select bank: {bankCode}")
    public PaymentPage selectBank(String bankCode) {
        log.info("Selecting bank: {}", bankCode);
        click(String.format(BANK_OPTION, bankCode));
        return this;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PAYMENT SUBMISSION
    // ═══════════════════════════════════════════════════════════════════════════

    @Step("Click Pay Now button")
    public void clickPayNow() {
        log.info("Clicking Pay Now button");
        click(PAY_NOW_BUTTON);

        // Wait for response - either processing, 3DS, success, or error
        waitFor(1000);
    }

    @Step("Complete payment with card")
    public OrderConfirmationPage payWithCard(TestCard card) {
        log.info("Completing payment with test card");

        selectPaymentMethod(PaymentMethod.CREDIT_CARD);
        enterCardDetails(card);
        clickPayNow();

        // Handle 3DS if required
        if (card.requires3DS() && is3DSPopupVisible()) {
            handle3DSAuthentication(card.getOtpCode());
        }

        // Check for error
        if (isPaymentErrorVisible()) {
            String error = getPaymentError();
            throw new PaymentFailedException("Payment failed: " + error);
        }

        // Wait for success
        waitForUrl(".*/order/confirmation.*", 30000);

        return new OrderConfirmationPage(page);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 3DS AUTHENTICATION
    // ═══════════════════════════════════════════════════════════════════════════

    @Step("Handle 3DS authentication")
    public PaymentPage handle3DSAuthentication(String otp) {
        log.info("Handling 3DS authentication");

        waitForVisible(THREE_DS_IFRAME);

        FrameLocator threeDSFrame = frameLocator(THREE_DS_IFRAME);
        threeDSFrame.locator(THREE_DS_OTP_INPUT).fill(otp);
        threeDSFrame.locator(THREE_DS_SUBMIT).click();

        // Wait for 3DS frame to close
        waitForHidden(THREE_DS_IFRAME, 30000);

        return this;
    }

    public boolean is3DSPopupVisible() {
        return isVisible(THREE_DS_IFRAME);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // STATUS CHECKS
    // ═══════════════════════════════════════════════════════════════════════════

    public boolean isPaymentProcessing() {
        return isVisible(PAYMENT_PROCESSING);
    }

    public boolean isPaymentErrorVisible() {
        return isVisible(PAYMENT_ERROR);
    }

    public boolean isPaymentSuccessful() {
        return isVisible(PAYMENT_SUCCESS);
    }

    public String getPaymentError() {
        if (isPaymentErrorVisible()) {
            return getText(PAYMENT_ERROR);
        }
        return null;
    }

    public String getOrderTotal() {
        return getText(ORDER_TOTAL);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // EXCEPTIONS
    // ═══════════════════════════════════════════════════════════════════════════

    public static class PaymentFailedException extends RuntimeException {
        public PaymentFailedException(String message) {
            super(message);
        }
    }
}
