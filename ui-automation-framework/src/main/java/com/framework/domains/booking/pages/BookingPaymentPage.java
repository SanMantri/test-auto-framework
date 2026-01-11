package com.framework.domains.booking.pages;

import com.framework.core.base.BasePage;
import com.framework.domains.payments.models.PaymentMethod;
import com.framework.domains.payments.models.TestCard;
import com.microsoft.playwright.FrameLocator;
import com.microsoft.playwright.Page;
import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;

/**
 * BookingPaymentPage - Payment page for movie ticket booking
 *
 * Handles:
 * - Contact information entry
 * - Payment method selection
 * - Card/UPI payment
 * - Coupon application
 * - Payment submission
 */
@Slf4j
public class BookingPaymentPage extends BasePage {

    // ═══════════════════════════════════════════════════════════════════════════
    // LOCATORS
    // ═══════════════════════════════════════════════════════════════════════════

    // Booking Summary
    private static final String BOOKING_SUMMARY = "[data-testid='booking-summary']";
    private static final String MOVIE_TITLE = "[data-testid='summary-movie']";
    private static final String SHOW_INFO = "[data-testid='summary-show']";
    private static final String SEATS_INFO = "[data-testid='summary-seats']";
    private static final String AMOUNT_INFO = "[data-testid='summary-amount']";

    // Timer
    private static final String PAYMENT_TIMER = "[data-testid='payment-timer']";
    private static final String TIMER_EXPIRED = "[data-testid='timer-expired']";

    // Contact Information
    private static final String EMAIL_INPUT = "[data-testid='email-input'], input[name='email']";
    private static final String PHONE_INPUT = "[data-testid='phone-input'], input[name='phone']";

    // Payment Methods
    private static final String PAYMENT_METHOD_OPTION = "[data-testid='payment-%s']";
    private static final String SELECTED_PAYMENT = "[data-testid='selected-payment']";

    // Card Payment
    private static final String CARD_NUMBER_INPUT = "[data-testid='card-number']";
    private static final String CARD_EXPIRY_INPUT = "[data-testid='card-expiry']";
    private static final String CARD_CVV_INPUT = "[data-testid='card-cvv']";
    private static final String CARD_IFRAME = "iframe[data-testid='card-iframe']";

    // UPI Payment
    private static final String UPI_ID_INPUT = "[data-testid='upi-id']";
    private static final String VERIFY_UPI_BUTTON = "[data-testid='verify-upi']";
    private static final String UPI_VERIFIED = "[data-testid='upi-verified']";

    // Wallet Payment
    private static final String WALLET_OPTION = "[data-testid='wallet-%s']";
    private static final String WALLET_BALANCE = "[data-testid='wallet-balance']";

    // Coupon
    private static final String COUPON_INPUT = "[data-testid='coupon-input']";
    private static final String APPLY_COUPON = "[data-testid='apply-coupon']";
    private static final String COUPON_APPLIED = "[data-testid='coupon-applied']";
    private static final String COUPON_ERROR = "[data-testid='coupon-error']";
    private static final String REMOVE_COUPON = "[data-testid='remove-coupon']";
    private static final String DISCOUNT_AMOUNT = "[data-testid='discount-amount']";

    // Price Breakdown
    private static final String TICKET_PRICE = "[data-testid='ticket-price']";
    private static final String CONVENIENCE_FEE = "[data-testid='convenience-fee']";
    private static final String GST = "[data-testid='gst']";
    private static final String TOTAL_AMOUNT = "[data-testid='total-amount']";

    // Actions
    private static final String PAY_BUTTON = "[data-testid='pay-btn'], button:has-text('Pay')";
    private static final String CANCEL_BUTTON = "[data-testid='cancel-btn']";

    // Status
    private static final String PAYMENT_PROCESSING = "[data-testid='payment-processing']";
    private static final String PAYMENT_ERROR = "[data-testid='payment-error']";

    // 3DS
    private static final String THREE_DS_IFRAME = "iframe[data-testid='3ds-frame']";
    private static final String THREE_DS_OTP_INPUT = "#otp-input";
    private static final String THREE_DS_SUBMIT = "#submit-otp";

    // ═══════════════════════════════════════════════════════════════════════════
    // CONSTRUCTOR
    // ═══════════════════════════════════════════════════════════════════════════

    public BookingPaymentPage(Page page) {
        super(page);
    }

    @Override
    protected void waitForPageLoad() {
        waitForVisible(BOOKING_SUMMARY);
        waitForVisible(PAY_BUTTON);
    }

    @Override
    public boolean isDisplayed() {
        return isVisible(BOOKING_SUMMARY) && isVisible(PAY_BUTTON);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // BOOKING SUMMARY
    // ═══════════════════════════════════════════════════════════════════════════

    public String getMovieTitle() {
        return getText(MOVIE_TITLE);
    }

    public String getShowInfo() {
        return getText(SHOW_INFO);
    }

    public String getSeatsInfo() {
        return getText(SEATS_INFO);
    }

    public String getTotalAmount() {
        return getText(TOTAL_AMOUNT);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TIMER
    // ═══════════════════════════════════════════════════════════════════════════

    public String getRemainingTime() {
        return getText(PAYMENT_TIMER);
    }

    public boolean isTimerExpired() {
        return isVisible(TIMER_EXPIRED);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CONTACT INFORMATION
    // ═══════════════════════════════════════════════════════════════════════════

    @Step("Enter email: {email}")
    public BookingPaymentPage enterEmail(String email) {
        log.info("Entering email: {}", email);
        fill(EMAIL_INPUT, email);
        return this;
    }

    @Step("Enter phone: {phone}")
    public BookingPaymentPage enterPhone(String phone) {
        log.info("Entering phone: {}", phone);
        fill(PHONE_INPUT, phone);
        return this;
    }

    @Step("Enter contact information")
    public BookingPaymentPage enterContactInfo(String email, String phone) {
        enterEmail(email);
        enterPhone(phone);
        return this;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PAYMENT METHOD SELECTION
    // ═══════════════════════════════════════════════════════════════════════════

    @Step("Select payment method: {method}")
    public BookingPaymentPage selectPaymentMethod(PaymentMethod method) {
        log.info("Selecting payment method: {}", method);
        click(String.format(PAYMENT_METHOD_OPTION, method.getValue()));
        return this;
    }

    public String getSelectedPaymentMethod() {
        return getAttribute(SELECTED_PAYMENT, "data-method");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CARD PAYMENT
    // ═══════════════════════════════════════════════════════════════════════════

    @Step("Enter card details")
    public BookingPaymentPage enterCardDetails(TestCard card) {
        log.info("Entering card details");

        if (isVisible(CARD_IFRAME)) {
            FrameLocator frame = frameLocator(CARD_IFRAME);
            frame.locator(CARD_NUMBER_INPUT).fill(card.getNumber());
            frame.locator(CARD_EXPIRY_INPUT).fill(card.getExpiry());
            frame.locator(CARD_CVV_INPUT).fill(card.getCvv());
        } else {
            fill(CARD_NUMBER_INPUT, card.getNumber());
            fill(CARD_EXPIRY_INPUT, card.getExpiry());
            fill(CARD_CVV_INPUT, card.getCvv());
        }

        return this;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // UPI PAYMENT
    // ═══════════════════════════════════════════════════════════════════════════

    @Step("Enter UPI ID: {upiId}")
    public BookingPaymentPage enterUPIId(String upiId) {
        log.info("Entering UPI ID: {}", upiId);
        fill(UPI_ID_INPUT, upiId);
        return this;
    }

    @Step("Verify UPI ID")
    public BookingPaymentPage verifyUPI() {
        log.info("Verifying UPI ID");
        click(VERIFY_UPI_BUTTON);
        waitForVisible(UPI_VERIFIED);
        return this;
    }

    public boolean isUPIVerified() {
        return isVisible(UPI_VERIFIED);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // WALLET PAYMENT
    // ═══════════════════════════════════════════════════════════════════════════

    @Step("Select wallet: {walletName}")
    public BookingPaymentPage selectWallet(String walletName) {
        log.info("Selecting wallet: {}", walletName);
        click(String.format(WALLET_OPTION, walletName.toLowerCase()));
        return this;
    }

    public String getWalletBalance() {
        return getText(WALLET_BALANCE);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // COUPON
    // ═══════════════════════════════════════════════════════════════════════════

    @Step("Apply coupon: {couponCode}")
    public BookingPaymentPage applyCoupon(String couponCode) {
        log.info("Applying coupon: {}", couponCode);
        fill(COUPON_INPUT, couponCode);
        click(APPLY_COUPON);
        waitFor(1000);
        return this;
    }

    public boolean isCouponApplied() {
        return isVisible(COUPON_APPLIED);
    }

    public boolean hasCouponError() {
        return isVisible(COUPON_ERROR);
    }

    public String getCouponError() {
        return getText(COUPON_ERROR);
    }

    @Step("Remove coupon")
    public BookingPaymentPage removeCoupon() {
        log.info("Removing coupon");
        click(REMOVE_COUPON);
        waitForHidden(COUPON_APPLIED);
        return this;
    }

    public String getDiscountAmount() {
        return getText(DISCOUNT_AMOUNT);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PRICE BREAKDOWN
    // ═══════════════════════════════════════════════════════════════════════════

    public String getTicketPrice() {
        return getText(TICKET_PRICE);
    }

    public String getConvenienceFee() {
        return getText(CONVENIENCE_FEE);
    }

    public String getGST() {
        return getText(GST);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PAYMENT SUBMISSION
    // ═══════════════════════════════════════════════════════════════════════════

    @Step("Click Pay button")
    public void clickPay() {
        log.info("Clicking Pay button");
        click(PAY_BUTTON);
        waitFor(1000);
    }

    @Step("Complete payment with card")
    public BookingConfirmationPage payWithCard(TestCard card, String email, String phone) {
        log.info("Completing payment with card");

        enterContactInfo(email, phone);
        selectPaymentMethod(PaymentMethod.CREDIT_CARD);
        enterCardDetails(card);
        clickPay();

        // Handle 3DS if required
        if (card.requires3DS() && is3DSVisible()) {
            handle3DS(card.getOtpCode());
        }

        // Check for error
        if (isPaymentErrorVisible()) {
            throw new PaymentFailedException(getPaymentError());
        }

        // Wait for confirmation
        waitForUrl(".*/booking/confirmation.*", 30000);

        return new BookingConfirmationPage(page);
    }

    @Step("Complete payment with UPI")
    public BookingConfirmationPage payWithUPI(String upiId, String email, String phone) {
        log.info("Completing payment with UPI");

        enterContactInfo(email, phone);
        selectPaymentMethod(PaymentMethod.UPI);
        enterUPIId(upiId);
        verifyUPI();
        clickPay();

        // Wait for UPI confirmation (can take longer)
        waitForUrl(".*/booking/confirmation.*", 60000);

        return new BookingConfirmationPage(page);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 3DS HANDLING
    // ═══════════════════════════════════════════════════════════════════════════

    public boolean is3DSVisible() {
        return isVisible(THREE_DS_IFRAME);
    }

    @Step("Handle 3DS authentication")
    public BookingPaymentPage handle3DS(String otp) {
        log.info("Handling 3DS authentication");

        waitForVisible(THREE_DS_IFRAME);

        FrameLocator frame = frameLocator(THREE_DS_IFRAME);
        frame.locator(THREE_DS_OTP_INPUT).fill(otp);
        frame.locator(THREE_DS_SUBMIT).click();

        waitForHidden(THREE_DS_IFRAME, 30000);

        return this;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ERROR HANDLING
    // ═══════════════════════════════════════════════════════════════════════════

    public boolean isPaymentProcessing() {
        return isVisible(PAYMENT_PROCESSING);
    }

    public boolean isPaymentErrorVisible() {
        return isVisible(PAYMENT_ERROR);
    }

    public String getPaymentError() {
        return getText(PAYMENT_ERROR);
    }

    @Step("Cancel payment")
    public SeatSelectionPage cancel() {
        log.info("Cancelling payment");
        click(CANCEL_BUTTON);
        return new SeatSelectionPage(page);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // EXCEPTION
    // ═══════════════════════════════════════════════════════════════════════════

    public static class PaymentFailedException extends RuntimeException {
        public PaymentFailedException(String message) {
            super(message);
        }
    }
}
