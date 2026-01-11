package com.framework.domains.booking.playbooks;

import com.framework.core.data.GlobalDataCache;
import com.framework.core.data.TestDataCache;
import com.framework.domains.booking.api.BookingApiClient;
import com.framework.domains.booking.api.BookingApiClient.SeatLockResponse;
import com.framework.domains.booking.api.MovieApiClient;
import com.framework.domains.booking.api.TheaterApiClient;
import com.framework.domains.booking.models.Movie;
import com.framework.domains.booking.models.ShowTime;
import com.framework.domains.booking.models.Theater;
import io.qameta.allure.Step;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * BookingSetupPlaybook - API-driven test setup for movie bookings
 *
 * This playbook provides fast test setup using API calls:
 * - Finding movies and show times
 * - Seat locking to prevent race conditions
 * - Test data preparation
 *
 * The key feature is seat locking - this ensures that seats selected
 * in tests won't be taken by other parallel tests or real users.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingSetupPlaybook {

    private final MovieApiClient movieApi;
    private final TheaterApiClient theaterApi;
    private final BookingApiClient bookingApi;
    private final GlobalDataCache globalDataCache;

    // ═══════════════════════════════════════════════════════════════════════════
    // MOVIE DISCOVERY
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Gets the first available movie for testing.
     */
    @Step("Get first available movie")
    public Movie getFirstAvailableMovie() {
        log.info("Getting first available movie");

        List<Movie> movies = movieApi.getNowShowing();
        if (movies.isEmpty()) {
            throw new RuntimeException("No movies currently showing");
        }

        Movie movie = movies.get(0);
        log.info("Found movie: {} ({})", movie.getTitle(), movie.getId());

        return movie;
    }

    /**
     * Gets a movie by genre.
     */
    @Step("Get movie by genre: {genre}")
    public Movie getMovieByGenre(String genre) {
        log.info("Getting movie by genre: {}", genre);

        List<Movie> movies = movieApi.getMoviesByGenre(genre);
        if (movies.isEmpty()) {
            throw new RuntimeException("No movies found for genre: " + genre);
        }

        return movies.get(0);
    }

    /**
     * Gets a specific movie by ID from global cache or API.
     */
    @Step("Get movie: {movieId}")
    public Movie getMovie(String movieId) {
        // Check cache first
        String cacheKey = "movie-" + movieId;
        Movie movie = (Movie) globalDataCache.get(cacheKey);

        if (movie == null) {
            movie = movieApi.getMovie(movieId);
            globalDataCache.put(cacheKey, movie);
        }

        return movie;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SHOW TIME DISCOVERY
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Gets first available show time for a movie today.
     */
    @Step("Get first available show time for movie: {movieId}")
    public ShowTime getFirstAvailableShowTime(String movieId) {
        return getFirstAvailableShowTime(movieId, LocalDate.now());
    }

    /**
     * Gets first available show time for a movie on a specific date.
     */
    @Step("Get first available show time for movie on date")
    public ShowTime getFirstAvailableShowTime(String movieId, LocalDate date) {
        log.info("Getting first available show time for movie: {} on {}", movieId, date);

        List<ShowTime> showTimes = movieApi.getShowTimes(movieId, date);

        ShowTime showTime = showTimes.stream()
            .filter(ShowTime::hasAvailability)
            .findFirst()
            .orElseThrow(() -> new RuntimeException(
                String.format("No available show times for movie %s on %s", movieId, date)));

        log.info("Found show time: {} at {}", showTime.getTime(), showTime.getTheaterId());

        return showTime;
    }

    /**
     * Gets first available show time at a specific theater.
     */
    @Step("Get show time at theater: {theaterId}")
    public ShowTime getShowTimeAtTheater(String movieId, String theaterId, LocalDate date) {
        log.info("Getting show time for movie {} at theater {} on {}", movieId, theaterId, date);

        List<ShowTime> showTimes = movieApi.getShowTimes(movieId, theaterId, date);

        return showTimes.stream()
            .filter(ShowTime::hasAvailability)
            .findFirst()
            .orElseThrow(() -> new RuntimeException(
                String.format("No available show times at theater %s", theaterId)));
    }

    /**
     * Gets show time with specific format (IMAX, 3D, etc.).
     */
    @Step("Get show time with format: {format}")
    public ShowTime getShowTimeByFormat(String movieId, LocalDate date, String format) {
        log.info("Getting {} show time for movie: {}", format, movieId);

        List<ShowTime> showTimes = movieApi.getShowTimesByFormat(movieId, date, format);

        return showTimes.stream()
            .filter(ShowTime::hasAvailability)
            .findFirst()
            .orElseThrow(() -> new RuntimeException(
                String.format("No available %s show times for movie %s", format, movieId)));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SEAT LOCKING (RACE CONDITION PROTECTION)
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Locks seats via API before UI selection.
     *
     * This is critical for parallel test execution - it ensures that
     * seats won't be taken by other tests or users between the time
     * we identify available seats and actually select them in the UI.
     *
     * @param showTimeId Show time ID
     * @param seatCount  Number of seats to lock
     * @return Lock response with lock ID and seat IDs
     */
    @Step("Lock {seatCount} seats for show: {showTimeId}")
    public SeatLockResponse lockSeats(String showTimeId, int seatCount) {
        log.info("Locking {} seats for show time: {}", seatCount, showTimeId);

        // Get available seats
        BookingApiClient.SeatLayout layout = bookingApi.getSeatLayout(showTimeId);

        List<String> availableSeats = layout.seats.stream()
            .filter(s -> s.isAvailable())
            .limit(seatCount)
            .map(s -> s.getId())
            .toList();

        if (availableSeats.size() < seatCount) {
            throw new RuntimeException(String.format(
                "Only %d seats available, requested %d", availableSeats.size(), seatCount));
        }

        // Lock the seats
        SeatLockResponse lock = bookingApi.lockSeats(showTimeId, availableSeats);

        log.info("Locked seats: {} (lock expires at {})", lock.lockedSeats, lock.expiresAt);

        return lock;
    }

    /**
     * Locks specific seats by ID.
     */
    @Step("Lock specific seats")
    public SeatLockResponse lockSpecificSeats(String showTimeId, List<String> seatIds) {
        log.info("Locking specific seats: {} for show: {}", seatIds, showTimeId);

        // Verify seats are available
        BookingApiClient.SeatAvailabilityResponse availability =
            bookingApi.checkSeatAvailability(showTimeId, seatIds);

        if (!availability.allAvailable) {
            throw new RuntimeException(
                "Some seats are not available: " + availability.unavailableSeats);
        }

        return bookingApi.lockSeats(showTimeId, seatIds);
    }

    /**
     * Releases locked seats (cleanup).
     */
    @Step("Release seat lock: {lockId}")
    public void releaseLock(String lockId) {
        log.info("Releasing seat lock: {}", lockId);
        bookingApi.releaseSeats(lockId);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // COMPLETE SETUP SCENARIOS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Sets up a complete booking scenario with locked seats.
     *
     * Returns all necessary data for UI test execution.
     */
    @Step("Setup booking scenario with {seatCount} seats")
    public BookingTestData setupBookingScenario(int seatCount, TestDataCache testData) {
        log.info("Setting up booking scenario with {} seats", seatCount);

        // Get movie
        Movie movie = getFirstAvailableMovie();
        testData.put("movie", movie);
        testData.put("movieId", movie.getId());

        // Get show time
        ShowTime showTime = getFirstAvailableShowTime(movie.getId());
        testData.put("showTime", showTime);
        testData.put("showTimeId", showTime.getId());

        // Lock seats
        SeatLockResponse lock = lockSeats(showTime.getId(), seatCount);
        testData.put("seatLock", lock);
        testData.put("lockId", lock.lockId);
        testData.put("lockedSeats", lock.lockedSeats);

        BookingTestData data = new BookingTestData();
        data.movie = movie;
        data.showTime = showTime;
        data.seatLock = lock;

        log.info("Booking scenario setup complete: movie={}, showTime={}, seats={}",
            movie.getTitle(), showTime.getTime(), lock.lockedSeats);

        return data;
    }

    /**
     * Sets up scenario for specific theater/movie combination.
     */
    @Step("Setup booking at theater: {theaterId}")
    public BookingTestData setupBookingAtTheater(
            String movieId, String theaterId, int seatCount, TestDataCache testData) {

        Movie movie = getMovie(movieId);
        ShowTime showTime = getShowTimeAtTheater(movieId, theaterId, LocalDate.now());
        SeatLockResponse lock = lockSeats(showTime.getId(), seatCount);

        testData.put("movie", movie);
        testData.put("showTime", showTime);
        testData.put("seatLock", lock);

        BookingTestData data = new BookingTestData();
        data.movie = movie;
        data.showTime = showTime;
        data.seatLock = lock;

        return data;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // THEATER DISCOVERY
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Gets first theater in a city.
     */
    @Step("Get theater in city: {city}")
    public Theater getTheaterInCity(String city) {
        log.info("Getting theater in city: {}", city);

        List<Theater> theaters = theaterApi.getTheatersByCity(city);
        if (theaters.isEmpty()) {
            throw new RuntimeException("No theaters found in: " + city);
        }

        return theaters.get(0);
    }

    /**
     * Gets theater with specific format (IMAX, 4DX, etc.).
     */
    @Step("Get theater with format: {format}")
    public Theater getTheaterWithFormat(String city, String format) {
        log.info("Getting theater with {} in {}", format, city);

        List<Theater> theaters = theaterApi.getTheatersWithFormat(city, format);
        if (theaters.isEmpty()) {
            throw new RuntimeException(
                String.format("No theaters with %s in %s", format, city));
        }

        return theaters.get(0);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DATA CLASS
    // ═══════════════════════════════════════════════════════════════════════════

    public static class BookingTestData {
        public Movie movie;
        public ShowTime showTime;
        public Theater theater;
        public SeatLockResponse seatLock;

        public String getMovieId() {
            return movie != null ? movie.getId() : null;
        }

        public String getShowTimeId() {
            return showTime != null ? showTime.getId() : null;
        }

        public List<String> getLockedSeats() {
            return seatLock != null ? seatLock.lockedSeats : List.of();
        }

        public String getLockId() {
            return seatLock != null ? seatLock.lockId : null;
        }
    }
}
