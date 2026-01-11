package com.framework.domains.booking.pages;

import com.framework.core.base.BasePage;
import com.microsoft.playwright.Page;
import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;

/**
 * BookingConfirmationPage - Booking confirmation after successful payment
 *
 * Displays:
 * - Booking number and QR code
 * - Movie and show details
 * - Seat information
 * - Payment summary
 * - Download/share options
 */
@Slf4j
public class BookingConfirmationPage extends BasePage {

    // ═══════════════════════════════════════════════════════════════════════════
    // LOCATORS
    // ═══════════════════════════════════════════════════════════════════════════

    // Success Indicator
    private static final String SUCCESS_ICON = "[data-testid='success-icon']";
    private static final String CONFIRMATION_MESSAGE = "[data-testid='confirmation-message']";

    // Booking Details
    private static final String BOOKING_NUMBER = "[data-testid='booking-number']";
    private static final String QR_CODE = "[data-testid='qr-code']";
    private static final String BOOKING_DATE = "[data-testid='booking-date']";

    // Movie Information
    private static final String MOVIE_TITLE = "[data-testid='movie-title']";
    private static final String MOVIE_FORMAT = "[data-testid='movie-format']";
    private static final String MOVIE_LANGUAGE = "[data-testid='movie-language']";

    // Show Details
    private static final String THEATER_NAME = "[data-testid='theater-name']";
    private static final String THEATER_ADDRESS = "[data-testid='theater-address']";
    private static final String SHOW_DATE = "[data-testid='show-date']";
    private static final String SHOW_TIME = "[data-testid='show-time']";
    private static final String SCREEN_NAME = "[data-testid='screen-name']";

    // Seat Information
    private static final String SEAT_INFO = "[data-testid='seat-info']";
    private static final String SEAT_COUNT = "[data-testid='seat-count']";
    private static final String SEAT_NUMBERS = "[data-testid='seat-numbers']";
    private static final String SEAT_CATEGORY = "[data-testid='seat-category']";

    // Payment Summary
    private static final String PAYMENT_METHOD = "[data-testid='payment-method']";
    private static final String TRANSACTION_ID = "[data-testid='transaction-id']";
    private static final String TICKET_AMOUNT = "[data-testid='ticket-amount']";
    private static final String CONVENIENCE_FEE = "[data-testid='convenience-fee']";
    private static final String GST_AMOUNT = "[data-testid='gst-amount']";
    private static final String DISCOUNT = "[data-testid='discount']";
    private static final String TOTAL_PAID = "[data-testid='total-paid']";

    // Contact Information
    private static final String EMAIL_SENT = "[data-testid='email-sent']";
    private static final String SMS_SENT = "[data-testid='sms-sent']";

    // Actions
    private static final String DOWNLOAD_TICKET = "[data-testid='download-ticket']";
    private static final String SHARE_BOOKING = "[data-testid='share-booking']";
    private static final String ADD_TO_CALENDAR = "[data-testid='add-to-calendar']";
    private static final String VIEW_ALL_BOOKINGS = "[data-testid='view-bookings']";
    private static final String BOOK_MORE = "[data-testid='book-more']";

    // M-Ticket
    private static final String M_TICKET_AVAILABLE = "[data-testid='m-ticket-available']";
    private static final String VIEW_M_TICKET = "[data-testid='view-m-ticket']";

    // ═══════════════════════════════════════════════════════════════════════════
    // CONSTRUCTOR
    // ═══════════════════════════════════════════════════════════════════════════

    public BookingConfirmationPage(Page page) {
        super(page);
    }

    @Override
    protected void waitForPageLoad() {
        waitForVisible(SUCCESS_ICON);
        waitForVisible(BOOKING_NUMBER);
    }

    @Override
    public boolean isDisplayed() {
        return isVisible(SUCCESS_ICON) && isVisible(BOOKING_NUMBER);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // BOOKING DETAILS
    // ═══════════════════════════════════════════════════════════════════════════

    @Step("Get booking number")
    public String getBookingNumber() {
        String text = getText(BOOKING_NUMBER);
        return text.replaceAll(".*#", "").replaceAll("Booking.*:", "").trim();
    }

    public String getConfirmationMessage() {
        return getText(CONFIRMATION_MESSAGE);
    }

    public boolean isBookingConfirmed() {
        return isVisible(SUCCESS_ICON) && isVisible(QR_CODE);
    }

    public boolean hasQRCode() {
        return isVisible(QR_CODE);
    }

    public String getBookingDate() {
        return getText(BOOKING_DATE);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // MOVIE INFORMATION
    // ═══════════════════════════════════════════════════════════════════════════

    public String getMovieTitle() {
        return getText(MOVIE_TITLE);
    }

    public String getMovieFormat() {
        return getText(MOVIE_FORMAT);
    }

    public String getMovieLanguage() {
        return getText(MOVIE_LANGUAGE);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SHOW DETAILS
    // ═══════════════════════════════════════════════════════════════════════════

    public String getTheaterName() {
        return getText(THEATER_NAME);
    }

    public String getTheaterAddress() {
        return getText(THEATER_ADDRESS);
    }

    public String getShowDate() {
        return getText(SHOW_DATE);
    }

    public String getShowTime() {
        return getText(SHOW_TIME);
    }

    public String getScreenName() {
        return getText(SCREEN_NAME);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SEAT INFORMATION
    // ═══════════════════════════════════════════════════════════════════════════

    public String getSeatCount() {
        return getText(SEAT_COUNT);
    }

    public String getSeatNumbers() {
        return getText(SEAT_NUMBERS);
    }

    public String getSeatCategory() {
        return getText(SEAT_CATEGORY);
    }

    public int getNumberOfTickets() {
        String count = getSeatCount().replaceAll("[^0-9]", "");
        return Integer.parseInt(count);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PAYMENT SUMMARY
    // ═══════════════════════════════════════════════════════════════════════════

    public String getPaymentMethod() {
        return getText(PAYMENT_METHOD);
    }

    public String getTransactionId() {
        return getText(TRANSACTION_ID);
    }

    public String getTicketAmount() {
        return getText(TICKET_AMOUNT);
    }

    public String getConvenienceFee() {
        return getText(CONVENIENCE_FEE);
    }

    public String getGSTAmount() {
        return getText(GST_AMOUNT);
    }

    public String getDiscount() {
        if (isVisible(DISCOUNT)) {
            return getText(DISCOUNT);
        }
        return null;
    }

    public String getTotalPaid() {
        return getText(TOTAL_PAID);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // NOTIFICATIONS
    // ═══════════════════════════════════════════════════════════════════════════

    public boolean isEmailSent() {
        return isVisible(EMAIL_SENT);
    }

    public boolean isSMSSent() {
        return isVisible(SMS_SENT);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // M-TICKET
    // ═══════════════════════════════════════════════════════════════════════════

    public boolean isMTicketAvailable() {
        return isVisible(M_TICKET_AVAILABLE);
    }

    @Step("View M-Ticket")
    public void viewMTicket() {
        log.info("Viewing M-Ticket");
        click(VIEW_M_TICKET);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ACTIONS
    // ═══════════════════════════════════════════════════════════════════════════

    @Step("Download ticket")
    public void downloadTicket() {
        log.info("Downloading ticket");
        click(DOWNLOAD_TICKET);
        waitFor(2000); // Wait for download
    }

    @Step("Share booking")
    public void shareBooking() {
        log.info("Sharing booking");
        click(SHARE_BOOKING);
    }

    @Step("Add to calendar")
    public void addToCalendar() {
        log.info("Adding to calendar");
        click(ADD_TO_CALENDAR);
    }

    @Step("View all bookings")
    public void viewAllBookings() {
        log.info("Viewing all bookings");
        click(VIEW_ALL_BOOKINGS);
    }

    @Step("Book more tickets")
    public MovieListingPage bookMore() {
        log.info("Booking more tickets");
        click(BOOK_MORE);
        return new MovieListingPage(page);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // VERIFICATION HELPERS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Gets complete booking summary for verification
     */
    public String getBookingSummary() {
        return String.format(
            "Booking: %s | Movie: %s | Date: %s %s | Seats: %s | Total: %s",
            getBookingNumber(),
            getMovieTitle(),
            getShowDate(),
            getShowTime(),
            getSeatNumbers(),
            getTotalPaid()
        );
    }

    /**
     * Verifies all essential booking details are present
     */
    public boolean hasCompleteDetails() {
        return isVisible(BOOKING_NUMBER) &&
               isVisible(MOVIE_TITLE) &&
               isVisible(SHOW_DATE) &&
               isVisible(SHOW_TIME) &&
               isVisible(SEAT_NUMBERS) &&
               isVisible(TOTAL_PAID) &&
               isVisible(QR_CODE);
    }
}
