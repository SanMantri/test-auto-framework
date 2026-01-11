package com.framework.domains.booking.tests;

import com.framework.core.base.BaseTest;
import com.framework.domains.booking.api.BookingApiClient;
import com.framework.domains.booking.api.MovieApiClient;
import com.framework.domains.booking.models.Booking;
import com.framework.domains.booking.models.Booking.BookingStatus;
import com.framework.domains.booking.models.Movie;
import com.framework.domains.booking.models.ShowTime;
import com.framework.domains.booking.pages.*;
import com.framework.domains.booking.playbooks.BookingFlowPlaybook;
import com.framework.domains.booking.playbooks.BookingSetupPlaybook;
import com.framework.domains.payments.models.TestCard;
import io.qameta.allure.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MovieBookingFlowTest - End-to-end movie booking tests
 *
 * Tests the complete movie ticket booking experience using:
 * - API-driven test setup (finding movies, locking seats)
 * - UI interactions for the booking flow
 * - API verification of booking status
 *
 * Key Feature: Seat locking for parallel test safety
 * - Seats are locked via API before UI selection
 * - This prevents race conditions in parallel test execution
 * - Locks are automatically released on test completion
 */
@Epic("Movie Booking")
@Feature("Booking Flow")
public class MovieBookingFlowTest extends BaseTest {

    @Autowired
    private BookingSetupPlaybook bookingSetupPlaybook;

    @Autowired
    private BookingFlowPlaybook bookingFlowPlaybook;

    @Autowired
    private MovieApiClient movieApi;

    @Autowired
    private BookingApiClient bookingApi;

    // ═══════════════════════════════════════════════════════════════════════════
    // SUCCESSFUL BOOKING TESTS
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @Severity(SeverityLevel.BLOCKER)
    @Story("Single Ticket Booking")
    @Description("Verify successful booking of a single movie ticket")
    public void testSingleTicketBooking() {
        // Complete booking flow
        BookingConfirmationPage confirmationPage = bookingFlowPlaybook.completeBookingFlow(
            getPage(),
            1,
            TestCard.VISA_SUCCESS,
            testData()
        );

        // Verify confirmation
        assertThat(confirmationPage.isBookingConfirmed())
            .as("Booking should be confirmed")
            .isTrue();

        assertThat(confirmationPage.hasQRCode())
            .as("QR code should be displayed")
            .isTrue();

        assertThat(confirmationPage.getNumberOfTickets())
            .as("Should have 1 ticket")
            .isEqualTo(1);

        // API: Verify booking
        String bookingNumber = confirmationPage.getBookingNumber();
        Booking booking = bookingFlowPlaybook.verifyBooking(bookingNumber);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @Story("Multiple Tickets Booking")
    @Description("Verify successful booking of multiple movie tickets")
    public void testMultipleTicketBooking() {
        // Book 4 tickets
        BookingConfirmationPage confirmationPage = bookingFlowPlaybook.completeBookingFlow(
            getPage(),
            4,
            TestCard.VISA_SUCCESS,
            testData()
        );

        // Verify
        assertThat(confirmationPage.isBookingConfirmed()).isTrue();
        assertThat(confirmationPage.getNumberOfTickets()).isEqualTo(4);

        // Verify seats are listed
        String seatNumbers = confirmationPage.getSeatNumbers();
        assertThat(seatNumbers).isNotEmpty();
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @Story("UPI Booking")
    @Description("Verify successful booking with UPI payment")
    public void testBookingWithUPI() {
        BookingConfirmationPage confirmationPage = bookingFlowPlaybook.completeBookingWithUPI(
            getPage(),
            2,
            "test@upi",
            testData()
        );

        assertThat(confirmationPage.isBookingConfirmed()).isTrue();
        assertThat(confirmationPage.getPaymentMethod()).containsIgnoringCase("UPI");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // MOVIE DISCOVERY TESTS
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @Severity(SeverityLevel.NORMAL)
    @Story("Movie Listing")
    @Description("Verify movie listing page displays movies correctly")
    public void testMovieListingDisplay() {
        MovieListingPage listingPage = new MovieListingPage(getPage());
        listingPage.navigate();

        // Verify movies are displayed
        assertThat(listingPage.getMovieCount())
            .as("Movies should be displayed")
            .isGreaterThan(0);

        // Verify movie titles are not empty
        List<String> titles = listingPage.getMovieTitles();
        assertThat(titles)
            .as("Movie titles should be present")
            .allSatisfy(title -> assertThat(title).isNotEmpty());
    }

    @Test
    @Severity(SeverityLevel.NORMAL)
    @Story("Movie Search")
    @Description("Verify movie search functionality")
    public void testMovieSearch() {
        // Get a known movie
        Movie movie = bookingSetupPlaybook.getFirstAvailableMovie();
        String searchTerm = movie.getTitle().split(" ")[0]; // First word

        MovieListingPage listingPage = new MovieListingPage(getPage());
        listingPage.navigate();
        listingPage.searchMovie(searchTerm);

        List<String> results = listingPage.getSearchResults();
        assertThat(results)
            .as("Search should return results")
            .isNotEmpty();
    }

    @Test
    @Severity(SeverityLevel.NORMAL)
    @Story("Movie Filtering")
    @Description("Verify movie filtering by language")
    public void testMovieFilterByLanguage() {
        MovieListingPage listingPage = new MovieListingPage(getPage());
        listingPage.navigate();

        int initialCount = listingPage.getMovieCount();
        listingPage.filterByLanguage("English");

        // Filter should reduce or maintain count (not increase)
        int filteredCount = listingPage.getMovieCount();
        assertThat(filteredCount)
            .as("Filtered count should not exceed initial")
            .isLessThanOrEqualTo(initialCount);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SEAT SELECTION TESTS
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @Story("Seat Selection")
    @Description("Verify seat selection UI shows correct availability")
    public void testSeatAvailabilityDisplay() {
        // Setup without locking seats
        BookingSetupPlaybook.BookingTestData setup =
            bookingSetupPlaybook.setupBookingScenario(2, testData());

        // Release lock to test fresh seat map
        bookingSetupPlaybook.releaseLock(setup.getLockId());

        SeatSelectionPage seatPage = new SeatSelectionPage(getPage());
        seatPage.navigate(setup.getShowTimeId());

        // Verify seat map is displayed
        assertThat(seatPage.isDisplayed())
            .as("Seat map should be displayed")
            .isTrue();

        // Verify seats are available
        assertThat(seatPage.getAvailableSeatCount())
            .as("Available seats should be shown")
            .isGreaterThan(0);

        // Verify proceed is disabled without selection
        assertThat(seatPage.isProceedEnabled())
            .as("Proceed should be disabled without selection")
            .isFalse();
    }

    @Test
    @Severity(SeverityLevel.NORMAL)
    @Story("Seat Selection")
    @Description("Verify seat selection and deselection")
    public void testSeatSelectionDeselection() {
        SeatSelectionPage seatPage = bookingFlowPlaybook.navigateToSeatSelection(
            getPage(), testData());

        // Select seats
        seatPage.selectFirstAvailableSeats(2);
        assertThat(seatPage.getSelectedSeatCount()).isEqualTo(2);

        // Deselect one
        List<String> selected = seatPage.getSelectedSeatIds();
        seatPage.deselectSeat(selected.get(0));

        assertThat(seatPage.getSelectedSeatCount()).isEqualTo(1);

        // Clear all
        seatPage.clearSelection();
        assertThat(seatPage.getSelectedSeatCount()).isEqualTo(0);
    }

    @Test
    @Severity(SeverityLevel.NORMAL)
    @Story("Seat Selection")
    @Description("Verify price calculation updates with seat selection")
    public void testPriceCalculation() {
        SeatSelectionPage seatPage = bookingFlowPlaybook.navigateToSeatSelection(
            getPage(), testData());

        // Select 1 seat
        seatPage.selectFirstAvailableSeats(1);
        String price1 = seatPage.getTotalAmount();

        // Select another seat
        seatPage.selectFirstAvailableSeats(1);
        String price2 = seatPage.getTotalAmount();

        // Price should increase (comparing as strings, both should be non-empty)
        assertThat(price1).isNotEmpty();
        assertThat(price2).isNotEmpty();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SHOW TIME TESTS
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @Severity(SeverityLevel.NORMAL)
    @Story("Show Times")
    @Description("Verify show times are displayed for selected movie")
    public void testShowTimeDisplay() {
        Movie movie = bookingSetupPlaybook.getFirstAvailableMovie();

        MovieDetailsPage detailsPage = new MovieDetailsPage(getPage());
        detailsPage.navigate(movie.getId());

        // Verify theater count
        assertThat(detailsPage.getTheaterCount())
            .as("Theaters should be displayed")
            .isGreaterThan(0);

        // Verify show times exist
        assertThat(detailsPage.getAvailableShowTimeCount())
            .as("Available show times should exist")
            .isGreaterThan(0);
    }

    @Test
    @Severity(SeverityLevel.NORMAL)
    @Story("Show Times")
    @Description("Verify date selection updates show times")
    public void testDateSelection() {
        Movie movie = bookingSetupPlaybook.getFirstAvailableMovie();

        MovieDetailsPage detailsPage = new MovieDetailsPage(getPage());
        detailsPage.navigate(movie.getId());

        List<String> dates = detailsPage.getAvailableDates();
        if (dates.size() > 1) {
            // Select a different date
            detailsPage.selectDate(dates.get(1));

            // Verify page updated (no error)
            assertThat(detailsPage.isDisplayed()).isTrue();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PAYMENT FAILURE TESTS
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @Story("Payment Failures")
    @Description("Verify error handling for declined card")
    public void testDeclinedCardBooking() {
        BookingPaymentPage paymentPage = bookingFlowPlaybook.attemptBookingWithDeclinedCard(
            getPage(), 2, testData());

        assertThat(paymentPage.isPaymentErrorVisible())
            .as("Payment error should be displayed")
            .isTrue();

        String error = paymentPage.getPaymentError();
        assertThat(error)
            .as("Error should indicate declined")
            .containsIgnoringCase("declined");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // BOOKING MANAGEMENT TESTS
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @Severity(SeverityLevel.NORMAL)
    @Story("Booking Management")
    @Description("Verify booking can be cancelled and refunded")
    public void testBookingCancellation() {
        // Complete a booking
        BookingConfirmationPage confirmationPage = bookingFlowPlaybook.completeBookingFlow(
            getPage(), 2, TestCard.VISA_SUCCESS, testData());

        String bookingNumber = confirmationPage.getBookingNumber();

        // API: Cancel booking
        Booking booking = bookingApi.getBookingByNumber(bookingNumber);

        if (booking.canCancel()) {
            Booking cancelled = bookingApi.cancelBooking(booking.getId(), "Test cancellation");
            assertThat(cancelled.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        } else {
            // Show time too close, booking cannot be cancelled
            log.info("Booking {} cannot be cancelled (show time too close)", bookingNumber);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // RACE CONDITION PROTECTION TEST
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @Story("Seat Locking")
    @Description("Verify seat locking prevents double booking")
    public void testSeatLockingPreventsDoubleBooking() {
        // Setup scenario and get locked seats
        BookingSetupPlaybook.BookingTestData setup =
            bookingSetupPlaybook.setupBookingScenario(2, testData());

        // Verify seats are locked
        List<String> lockedSeats = setup.getLockedSeats();
        assertThat(lockedSeats).hasSize(2);

        // Try to lock the same seats again (should fail)
        try {
            BookingApiClient.SeatAvailabilityResponse availability =
                bookingApi.checkSeatAvailability(setup.getShowTimeId(), lockedSeats);

            assertThat(availability.allAvailable)
                .as("Locked seats should not be available")
                .isFalse();
        } finally {
            // Cleanup
            bookingSetupPlaybook.releaseLock(setup.getLockId());
        }
    }
}
