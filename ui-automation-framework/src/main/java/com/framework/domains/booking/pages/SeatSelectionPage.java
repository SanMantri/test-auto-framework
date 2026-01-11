package com.framework.domains.booking.pages;

import com.framework.core.base.BasePage;
import com.framework.domains.booking.models.Seat.SeatStatus;
import com.microsoft.playwright.Page;
import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * SeatSelectionPage - Interactive seat map and selection
 *
 * Handles:
 * - Seat map display
 * - Seat selection/deselection
 * - Category filtering
 * - Price calculation
 * - Timer management
 */
@Slf4j
public class SeatSelectionPage extends BasePage {

    // ═══════════════════════════════════════════════════════════════════════════
    // LOCATORS
    // ═══════════════════════════════════════════════════════════════════════════

    // Show Info
    private static final String SHOW_INFO = "[data-testid='show-info']";
    private static final String MOVIE_TITLE = "[data-testid='movie-title']";
    private static final String THEATER_NAME = "[data-testid='theater-name']";
    private static final String SHOW_DATE_TIME = "[data-testid='show-datetime']";

    // Seat Map
    private static final String SEAT_MAP = "[data-testid='seat-map']";
    private static final String SCREEN_INDICATOR = "[data-testid='screen-indicator']";
    private static final String SEAT = "[data-testid='seat-%s']";
    private static final String SEAT_ROW = "[data-testid='row-%s']";
    private static final String AVAILABLE_SEAT = "[data-testid^='seat-'].available";
    private static final String SELECTED_SEAT = "[data-testid^='seat-'].selected";
    private static final String BOOKED_SEAT = "[data-testid^='seat-'].booked";
    private static final String BLOCKED_SEAT = "[data-testid^='seat-'].blocked";

    // Seat Categories
    private static final String CATEGORY_LEGEND = "[data-testid='category-legend']";
    private static final String CATEGORY_ITEM = "[data-testid='category-%s']";
    private static final String CATEGORY_PRICE = "[data-testid='category-price-%s']";

    // Timer
    private static final String SELECTION_TIMER = "[data-testid='selection-timer']";
    private static final String TIMER_WARNING = "[data-testid='timer-warning']";

    // Selection Summary
    private static final String SELECTED_SEATS_LIST = "[data-testid='selected-seats']";
    private static final String SELECTED_COUNT = "[data-testid='selected-count']";
    private static final String TICKET_PRICE = "[data-testid='ticket-price']";
    private static final String CONVENIENCE_FEE = "[data-testid='convenience-fee']";
    private static final String TOTAL_AMOUNT = "[data-testid='total-amount']";

    // Quantity Selector (for some theaters)
    private static final String QUANTITY_SELECTOR = "[data-testid='quantity-selector']";
    private static final String QUANTITY_OPTION = "[data-testid='quantity-%d']";

    // Actions
    private static final String PROCEED_BUTTON = "[data-testid='proceed-btn'], button:has-text('Proceed')";
    private static final String CANCEL_BUTTON = "[data-testid='cancel-btn']";

    // Error/Warning
    private static final String MAX_SEATS_WARNING = "[data-testid='max-seats-warning']";
    private static final String NO_SEATS_ERROR = "[data-testid='no-seats-error']";
    private static final String SEAT_UNAVAILABLE_ERROR = "[data-testid='seat-unavailable']";

    // Loading
    private static final String LOADING = "[data-testid='loading']";
    private static final String SEAT_MAP_LOADING = "[data-testid='seat-map-loading']";

    // ═══════════════════════════════════════════════════════════════════════════
    // CONSTRUCTOR & NAVIGATION
    // ═══════════════════════════════════════════════════════════════════════════

    public SeatSelectionPage(Page page) {
        super(page);
    }

    @Step("Navigate to seat selection: {showTimeId}")
    public SeatSelectionPage navigate(String showTimeId) {
        navigateTo("/booking/seats/" + showTimeId);
        return this;
    }

    @Override
    protected void waitForPageLoad() {
        waitForHidden(SEAT_MAP_LOADING);
        waitForVisible(SEAT_MAP);
    }

    @Override
    public boolean isDisplayed() {
        return isVisible(SEAT_MAP);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SHOW INFORMATION
    // ═══════════════════════════════════════════════════════════════════════════

    public String getMovieTitle() {
        return getText(MOVIE_TITLE);
    }

    public String getTheaterName() {
        return getText(THEATER_NAME);
    }

    public String getShowDateTime() {
        return getText(SHOW_DATE_TIME);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SEAT SELECTION
    // ═══════════════════════════════════════════════════════════════════════════

    @Step("Select seat: {seatId}")
    public SeatSelectionPage selectSeat(String seatId) {
        log.info("Selecting seat: {}", seatId);

        String locator = String.format(SEAT, seatId);

        // Check if seat is available
        if (!isSeatAvailable(seatId)) {
            log.warn("Seat {} is not available", seatId);
            return this;
        }

        click(locator);
        waitFor(300); // Wait for selection animation

        return this;
    }

    @Step("Select multiple seats")
    public SeatSelectionPage selectSeats(List<String> seatIds) {
        log.info("Selecting {} seats: {}", seatIds.size(), seatIds);

        for (String seatId : seatIds) {
            selectSeat(seatId);
        }

        return this;
    }

    @Step("Deselect seat: {seatId}")
    public SeatSelectionPage deselectSeat(String seatId) {
        log.info("Deselecting seat: {}", seatId);

        if (isSeatSelected(seatId)) {
            click(String.format(SEAT, seatId));
            waitFor(300);
        }

        return this;
    }

    @Step("Clear all selections")
    public SeatSelectionPage clearSelection() {
        log.info("Clearing all seat selections");

        List<String> selectedSeats = getSelectedSeatIds();
        for (String seatId : selectedSeats) {
            deselectSeat(seatId);
        }

        return this;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SEAT STATUS CHECKS
    // ═══════════════════════════════════════════════════════════════════════════

    public boolean isSeatAvailable(String seatId) {
        String locator = String.format(SEAT, seatId);
        if (!isVisible(locator)) return false;

        String className = page.locator(locator).getAttribute("class");
        return className != null && className.contains("available");
    }

    public boolean isSeatSelected(String seatId) {
        String locator = String.format(SEAT, seatId);
        if (!isVisible(locator)) return false;

        String className = page.locator(locator).getAttribute("class");
        return className != null && className.contains("selected");
    }

    public boolean isSeatBooked(String seatId) {
        String locator = String.format(SEAT, seatId);
        if (!isVisible(locator)) return false;

        String className = page.locator(locator).getAttribute("class");
        return className != null && className.contains("booked");
    }

    public SeatStatus getSeatStatus(String seatId) {
        String locator = String.format(SEAT, seatId);
        if (!isVisible(locator)) return SeatStatus.UNAVAILABLE;

        String className = page.locator(locator).getAttribute("class");
        if (className.contains("selected")) return SeatStatus.SELECTED;
        if (className.contains("booked")) return SeatStatus.BOOKED;
        if (className.contains("blocked")) return SeatStatus.BLOCKED;
        if (className.contains("available")) return SeatStatus.AVAILABLE;
        return SeatStatus.UNAVAILABLE;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SEAT COUNTS
    // ═══════════════════════════════════════════════════════════════════════════

    public int getAvailableSeatCount() {
        return page.locator(AVAILABLE_SEAT).count();
    }

    public int getSelectedSeatCount() {
        return page.locator(SELECTED_SEAT).count();
    }

    public int getBookedSeatCount() {
        return page.locator(BOOKED_SEAT).count();
    }

    public List<String> getSelectedSeatIds() {
        List<String> seatIds = new ArrayList<>();
        page.locator(SELECTED_SEAT).all().forEach(el -> {
            String testId = el.getAttribute("data-testid");
            if (testId != null && testId.startsWith("seat-")) {
                seatIds.add(testId.replace("seat-", ""));
            }
        });
        return seatIds;
    }

    public List<String> getAvailableSeatIds() {
        List<String> seatIds = new ArrayList<>();
        page.locator(AVAILABLE_SEAT).all().forEach(el -> {
            String testId = el.getAttribute("data-testid");
            if (testId != null && testId.startsWith("seat-")) {
                seatIds.add(testId.replace("seat-", ""));
            }
        });
        return seatIds;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CATEGORY INFORMATION
    // ═══════════════════════════════════════════════════════════════════════════

    public String getCategoryPrice(String category) {
        return getText(String.format(CATEGORY_PRICE, category.toLowerCase()));
    }

    public boolean isCategoryAvailable(String category) {
        String locator = String.format(CATEGORY_ITEM, category.toLowerCase());
        return isVisible(locator);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TIMER
    // ═══════════════════════════════════════════════════════════════════════════

    public String getRemainingTime() {
        return getText(SELECTION_TIMER);
    }

    public boolean isTimerWarningVisible() {
        return isVisible(TIMER_WARNING);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PRICING
    // ═══════════════════════════════════════════════════════════════════════════

    public String getDisplayedSelectedCount() {
        return getText(SELECTED_COUNT);
    }

    public String getTicketPrice() {
        return getText(TICKET_PRICE);
    }

    public String getConvenienceFee() {
        return getText(CONVENIENCE_FEE);
    }

    public String getTotalAmount() {
        return getText(TOTAL_AMOUNT);
    }

    public BigDecimal getTotalAsDecimal() {
        String total = getTotalAmount().replaceAll("[^0-9.]", "");
        return new BigDecimal(total);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // QUANTITY SELECTOR
    // ═══════════════════════════════════════════════════════════════════════════

    public boolean hasQuantitySelector() {
        return isVisible(QUANTITY_SELECTOR);
    }

    @Step("Select quantity: {quantity}")
    public SeatSelectionPage selectQuantity(int quantity) {
        log.info("Selecting quantity: {}", quantity);
        click(String.format(QUANTITY_OPTION, quantity));
        return this;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ERROR HANDLING
    // ═══════════════════════════════════════════════════════════════════════════

    public boolean isMaxSeatsWarningVisible() {
        return isVisible(MAX_SEATS_WARNING);
    }

    public boolean isNoSeatsErrorVisible() {
        return isVisible(NO_SEATS_ERROR);
    }

    public boolean isSeatUnavailableErrorVisible() {
        return isVisible(SEAT_UNAVAILABLE_ERROR);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ACTIONS
    // ═══════════════════════════════════════════════════════════════════════════

    @Step("Proceed to payment")
    public BookingPaymentPage proceedToPayment() {
        log.info("Proceeding to payment with {} seats", getSelectedSeatCount());

        if (getSelectedSeatCount() == 0) {
            throw new IllegalStateException("No seats selected");
        }

        click(PROCEED_BUTTON);
        return new BookingPaymentPage(page);
    }

    @Step("Cancel seat selection")
    public MovieDetailsPage cancel() {
        log.info("Cancelling seat selection");
        click(CANCEL_BUTTON);
        return new MovieDetailsPage(page);
    }

    public boolean isProceedEnabled() {
        return page.locator(PROCEED_BUTTON).isEnabled();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // QUICK SELECTION HELPERS
    // ═══════════════════════════════════════════════════════════════════════════

    @Step("Select first {count} available seats")
    public SeatSelectionPage selectFirstAvailableSeats(int count) {
        log.info("Selecting first {} available seats", count);

        List<String> availableSeats = getAvailableSeatIds();
        if (availableSeats.size() < count) {
            throw new IllegalStateException(
                String.format("Only %d seats available, requested %d", availableSeats.size(), count));
        }

        for (int i = 0; i < count; i++) {
            selectSeat(availableSeats.get(i));
        }

        return this;
    }

    @Step("Select seats in row: {row}")
    public SeatSelectionPage selectSeatsInRow(String row, int count) {
        log.info("Selecting {} seats in row {}", count, row);

        String rowLocator = String.format(SEAT_ROW, row) + " " + AVAILABLE_SEAT;
        List<String> seatsInRow = new ArrayList<>();

        page.locator(rowLocator).all().forEach(el -> {
            String testId = el.getAttribute("data-testid");
            if (testId != null) {
                seatsInRow.add(testId.replace("seat-", ""));
            }
        });

        if (seatsInRow.size() < count) {
            throw new IllegalStateException(
                String.format("Only %d available seats in row %s, requested %d", seatsInRow.size(), row, count));
        }

        for (int i = 0; i < count; i++) {
            selectSeat(seatsInRow.get(i));
        }

        return this;
    }
}
