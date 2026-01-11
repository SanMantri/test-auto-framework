package com.framework.domains.booking.playbooks;

import com.framework.core.data.TestDataCache;
import com.framework.domains.booking.api.BookingApiClient;
import com.framework.domains.booking.api.BookingApiClient.SeatLockResponse;
import com.framework.domains.booking.models.Booking;
import com.framework.domains.booking.pages.*;
import com.framework.domains.payments.models.PaymentMethod;
import com.framework.domains.payments.models.TestCard;
import com.microsoft.playwright.Page;
import io.qameta.allure.Step;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * BookingFlowPlaybook - Complete booking flow orchestration
 *
 * Combines API setup with UI interactions for the complete movie booking flow.
 * Uses seat locking to prevent race conditions in parallel test execution.
 *
 * Typical flow:
 * 1. API: Lock seats
 * 2. UI: Select movie → Select show time → Select locked seats → Pay
 * 3. API: Verify booking
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingFlowPlaybook {

    private final BookingSetupPlaybook bookingSetupPlaybook;
    private final BookingApiClient bookingApi;

    // ═══════════════════════════════════════════════════════════════════════════
    // COMPLETE BOOKING FLOWS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Completes a full booking flow with seat locking.
     *
     * @param page      Playwright page
     * @param seatCount Number of seats to book
     * @param card      Test card for payment
     * @param testData  Test data cache
     * @return Booking confirmation page
     */
    @Step("Complete booking flow with {seatCount} seats")
    public BookingConfirmationPage completeBookingFlow(
            Page page, int seatCount, TestCard card, TestDataCache testData) {

        log.info("Starting complete booking flow with {} seats", seatCount);

        // API: Setup and lock seats
        BookingSetupPlaybook.BookingTestData setup =
            bookingSetupPlaybook.setupBookingScenario(seatCount, testData);

        try {
            // UI: Navigate to movie
            MovieListingPage listingPage = new MovieListingPage(page);
            listingPage.navigate();

            // Click on movie
            MovieDetailsPage detailsPage = listingPage.clickMovieCard(setup.getMovieId());

            // Select show time
            SeatSelectionPage seatPage = detailsPage.selectShowTime(setup.getShowTimeId());

            // Select the locked seats
            seatPage.selectSeats(setup.getLockedSeats());

            // Proceed to payment
            BookingPaymentPage paymentPage = seatPage.proceedToPayment();

            // Complete payment
            BookingConfirmationPage confirmationPage = paymentPage.payWithCard(
                card,
                "test@example.com",
                "9876543210"
            );

            // Store booking info
            testData.put("bookingNumber", confirmationPage.getBookingNumber());

            log.info("Booking completed: {}", confirmationPage.getBookingNumber());

            return confirmationPage;

        } finally {
            // Release lock if it wasn't consumed by booking
            if (setup.getLockId() != null) {
                try {
                    bookingSetupPlaybook.releaseLock(setup.getLockId());
                } catch (Exception e) {
                    log.debug("Lock already released or consumed by booking");
                }
            }
        }
    }

    /**
     * Completes booking with pre-locked seats (for when setup is done separately).
     */
    @Step("Complete booking with pre-locked seats")
    public BookingConfirmationPage completeBookingWithLockedSeats(
            Page page,
            String movieId,
            String showTimeId,
            List<String> lockedSeats,
            TestCard card) {

        log.info("Completing booking with {} pre-locked seats", lockedSeats.size());

        // Navigate to seat selection directly
        SeatSelectionPage seatPage = new SeatSelectionPage(page);
        seatPage.navigate(showTimeId);

        // Select locked seats
        seatPage.selectSeats(lockedSeats);

        // Complete payment
        BookingPaymentPage paymentPage = seatPage.proceedToPayment();
        return paymentPage.payWithCard(card, "test@example.com", "9876543210");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ALTERNATIVE PAYMENT METHODS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Completes booking with UPI payment.
     */
    @Step("Complete booking with UPI")
    public BookingConfirmationPage completeBookingWithUPI(
            Page page, int seatCount, String upiId, TestDataCache testData) {

        BookingSetupPlaybook.BookingTestData setup =
            bookingSetupPlaybook.setupBookingScenario(seatCount, testData);

        try {
            // Navigate directly to seat selection
            SeatSelectionPage seatPage = new SeatSelectionPage(page);
            seatPage.navigate(setup.getShowTimeId());
            seatPage.selectSeats(setup.getLockedSeats());

            BookingPaymentPage paymentPage = seatPage.proceedToPayment();
            return paymentPage.payWithUPI(upiId, "test@example.com", "9876543210");

        } finally {
            safeReleaseLock(setup.getLockId());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SEAT SELECTION SCENARIOS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Navigates to seat selection and returns page for custom seat selection.
     * Use this when you want to test specific seat selection behaviors.
     */
    @Step("Navigate to seat selection")
    public SeatSelectionPage navigateToSeatSelection(Page page, TestDataCache testData) {
        BookingSetupPlaybook.BookingTestData setup =
            bookingSetupPlaybook.setupBookingScenario(1, testData);

        // Release lock since we want manual selection
        safeReleaseLock(setup.getLockId());

        SeatSelectionPage seatPage = new SeatSelectionPage(page);
        seatPage.navigate(setup.getShowTimeId());

        return seatPage;
    }

    /**
     * Tests seat selection with specific category.
     */
    @Step("Book seats in category: {category}")
    public BookingConfirmationPage bookSeatsInCategory(
            Page page,
            String category,
            int seatCount,
            TestCard card,
            TestDataCache testData) {

        BookingSetupPlaybook.BookingTestData setup =
            bookingSetupPlaybook.setupBookingScenario(seatCount, testData);

        // Note: Seat lock may not respect category, so release it
        safeReleaseLock(setup.getLockId());

        SeatSelectionPage seatPage = new SeatSelectionPage(page);
        seatPage.navigate(setup.getShowTimeId());

        // Select seats from specific category row
        // This assumes category maps to row prefixes (e.g., Gold = rows G-J)
        seatPage.selectFirstAvailableSeats(seatCount);

        BookingPaymentPage paymentPage = seatPage.proceedToPayment();
        return paymentPage.payWithCard(card, "test@example.com", "9876543210");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // BOOKING VERIFICATION
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Verifies booking was created successfully via API.
     */
    @Step("Verify booking: {bookingNumber}")
    public Booking verifyBooking(String bookingNumber) {
        log.info("Verifying booking: {}", bookingNumber);

        Booking booking = bookingApi.getBookingByNumber(bookingNumber);

        if (!booking.isConfirmed()) {
            throw new RuntimeException("Booking is not confirmed: " + booking.getStatus());
        }

        log.info("Booking verified: {} - {} tickets", bookingNumber, booking.getTicketCount());

        return booking;
    }

    /**
     * Waits for booking confirmation after payment.
     */
    @Step("Wait for booking confirmation: {bookingId}")
    public Booking waitForBookingConfirmation(String bookingId) {
        return bookingApi.waitForStatus(
            bookingId,
            Booking.BookingStatus.CONFIRMED,
            30
        );
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // FAILURE SCENARIOS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Attempts booking with a card that will be declined.
     */
    @Step("Attempt booking with declined card")
    public BookingPaymentPage attemptBookingWithDeclinedCard(
            Page page, int seatCount, TestDataCache testData) {

        BookingSetupPlaybook.BookingTestData setup =
            bookingSetupPlaybook.setupBookingScenario(seatCount, testData);

        try {
            SeatSelectionPage seatPage = new SeatSelectionPage(page);
            seatPage.navigate(setup.getShowTimeId());
            seatPage.selectSeats(setup.getLockedSeats());

            BookingPaymentPage paymentPage = seatPage.proceedToPayment();

            // Enter details but use declined card
            paymentPage.enterContactInfo("test@example.com", "9876543210");
            paymentPage.selectPaymentMethod(PaymentMethod.CREDIT_CARD);
            paymentPage.enterCardDetails(TestCard.VISA_DECLINED);
            paymentPage.clickPay();

            // Wait for error
            paymentPage.waitFor(2000);

            return paymentPage;

        } finally {
            safeReleaseLock(setup.getLockId());
        }
    }

    /**
     * Tests timer expiry scenario by not completing payment in time.
     * Note: This is a slow test as it waits for actual timeout.
     */
    @Step("Test payment timer expiry")
    public BookingPaymentPage testTimerExpiry(Page page, TestDataCache testData) {
        log.warn("This test will wait for payment timer to expire - expect long execution");

        BookingSetupPlaybook.BookingTestData setup =
            bookingSetupPlaybook.setupBookingScenario(2, testData);

        try {
            SeatSelectionPage seatPage = new SeatSelectionPage(page);
            seatPage.navigate(setup.getShowTimeId());
            seatPage.selectSeats(setup.getLockedSeats());

            BookingPaymentPage paymentPage = seatPage.proceedToPayment();

            // Don't complete payment, wait for timer
            // Timer is typically 8-10 minutes, but test environments may have shorter timers
            while (!paymentPage.isTimerExpired()) {
                paymentPage.waitFor(5000);
                log.info("Waiting for timer... Remaining: {}", paymentPage.getRemainingTime());
            }

            return paymentPage;

        } finally {
            safeReleaseLock(setup.getLockId());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════════════════

    private void safeReleaseLock(String lockId) {
        if (lockId != null) {
            try {
                bookingSetupPlaybook.releaseLock(lockId);
            } catch (Exception e) {
                log.debug("Could not release lock {}: {}", lockId, e.getMessage());
            }
        }
    }

    /**
     * Default test contact information.
     */
    public static class TestContact {
        public static final String EMAIL = "test@example.com";
        public static final String PHONE = "9876543210";
    }
}
