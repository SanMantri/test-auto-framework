package com.framework.domains.booking.api;

import com.framework.core.base.BaseApiClient;
import com.framework.domains.booking.models.Booking;
import com.framework.domains.booking.models.Booking.BookingStatus;
import com.framework.domains.booking.models.Seat;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * BookingApiClient - API operations for movie bookings
 *
 * Used for:
 * - Seat availability checks
 * - Seat locking (race condition protection)
 * - Booking creation and management
 * - Test data cleanup
 */
@Slf4j
@Component
public class BookingApiClient extends BaseApiClient {

    @Override
    protected String getBasePath() {
        return "/api/v1/bookings";
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SEAT AVAILABILITY
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Gets seat layout and availability for a show time.
     */
    public SeatLayout getSeatLayout(String showTimeId) {
        log.info("Getting seat layout for show time: {}", showTimeId);
        Response response = get("/showtimes/" + showTimeId + "/seats");
        return getOkAs(response, SeatLayout.class);
    }

    /**
     * Checks if specific seats are available.
     */
    public SeatAvailabilityResponse checkSeatAvailability(String showTimeId, List<String> seatIds) {
        log.info("Checking availability for seats: {} in show: {}", seatIds, showTimeId);
        Response response = post("/showtimes/" + showTimeId + "/check-availability", Map.of(
            "seatIds", seatIds
        ));
        return getOkAs(response, SeatAvailabilityResponse.class);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SEAT LOCKING (RACE CONDITION PROTECTION)
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Locks seats temporarily before UI selection.
     * This prevents race conditions in tests by reserving seats via API first.
     *
     * @param showTimeId Show time ID
     * @param seatIds    Seats to lock
     * @return Lock response with lock ID and expiry
     */
    public SeatLockResponse lockSeats(String showTimeId, List<String> seatIds) {
        log.info("Locking seats {} for show time: {}", seatIds, showTimeId);
        Response response = post("/showtimes/" + showTimeId + "/lock", Map.of(
            "seatIds", seatIds
        ));
        return getOkAs(response, SeatLockResponse.class);
    }

    /**
     * Releases locked seats.
     */
    public void releaseSeats(String lockId) {
        log.info("Releasing seat lock: {}", lockId);
        delete("/locks/" + lockId);
    }

    /**
     * Extends seat lock duration.
     */
    public SeatLockResponse extendLock(String lockId, int additionalSeconds) {
        log.info("Extending lock: {} by {} seconds", lockId, additionalSeconds);
        Response response = post("/locks/" + lockId + "/extend", Map.of(
            "duration", additionalSeconds
        ));
        return getOkAs(response, SeatLockResponse.class);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // BOOKING OPERATIONS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Creates a booking (initiates payment).
     */
    public Booking createBooking(CreateBookingRequest request) {
        log.info("Creating booking for show: {} with {} seats", request.showTimeId, request.seatIds.size());
        Response response = post("", request);
        return getCreatedAs(response, Booking.class);
    }

    /**
     * Gets booking by ID.
     */
    public Booking getBooking(String bookingId) {
        log.info("Getting booking: {}", bookingId);
        Response response = get("/" + bookingId);
        return getOkAs(response, Booking.class);
    }

    /**
     * Gets booking by booking number.
     */
    public Booking getBookingByNumber(String bookingNumber) {
        log.info("Getting booking by number: {}", bookingNumber);
        Response response = get("/by-number/" + bookingNumber);
        return getOkAs(response, Booking.class);
    }

    /**
     * Gets all bookings for current user.
     */
    public List<Booking> getMyBookings() {
        log.info("Getting my bookings");
        Response response = get("/my-bookings");
        return List.of(getOkAs(response, Booking[].class));
    }

    /**
     * Cancels a booking.
     */
    public Booking cancelBooking(String bookingId) {
        log.info("Cancelling booking: {}", bookingId);
        Response response = post("/" + bookingId + "/cancel");
        return getOkAs(response, Booking.class);
    }

    /**
     * Cancels booking with reason.
     */
    public Booking cancelBooking(String bookingId, String reason) {
        log.info("Cancelling booking: {} with reason: {}", bookingId, reason);
        Response response = post("/" + bookingId + "/cancel", Map.of("reason", reason));
        return getOkAs(response, Booking.class);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // STATUS CHECKS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Waits for booking to reach a specific status.
     */
    public Booking waitForStatus(String bookingId, BookingStatus expectedStatus, int maxWaitSeconds) {
        log.info("Waiting for booking {} to reach status: {}", bookingId, expectedStatus);

        long startTime = System.currentTimeMillis();
        long maxWaitMs = maxWaitSeconds * 1000L;

        while (System.currentTimeMillis() - startTime < maxWaitMs) {
            Booking booking = getBooking(bookingId);
            if (booking.getStatus() == expectedStatus) {
                log.info("Booking reached status: {}", expectedStatus);
                return booking;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while waiting for booking status", e);
            }
        }

        throw new RuntimeException(String.format(
            "Booking %s did not reach status %s within %d seconds",
            bookingId, expectedStatus, maxWaitSeconds));
    }

    /**
     * Checks if booking is confirmed.
     */
    public boolean isBookingConfirmed(String bookingId) {
        Booking booking = getBooking(bookingId);
        return booking.isConfirmed();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // HELPER CLASSES
    // ═══════════════════════════════════════════════════════════════════════════

    public static class SeatLayout {
        public String showTimeId;
        public int rows;
        public int columns;
        public List<Seat> seats;
        public List<SeatCategory> categories;

        public static class SeatCategory {
            public String name;
            public java.math.BigDecimal price;
            public List<String> rows;
        }
    }

    public static class SeatAvailabilityResponse {
        public boolean allAvailable;
        public List<String> availableSeats;
        public List<String> unavailableSeats;
    }

    public static class SeatLockResponse {
        public String lockId;
        public List<String> lockedSeats;
        public java.time.LocalDateTime expiresAt;
        public int remainingSeconds;
    }

    public static class CreateBookingRequest {
        public String showTimeId;
        public List<String> seatIds;
        public String couponCode;
        public ContactInfo contact;

        public static class ContactInfo {
            public String email;
            public String phone;
        }
    }
}
