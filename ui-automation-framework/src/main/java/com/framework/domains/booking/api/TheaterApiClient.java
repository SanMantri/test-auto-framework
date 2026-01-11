package com.framework.domains.booking.api;

import com.framework.core.base.BaseApiClient;
import com.framework.domains.booking.models.ShowTime;
import com.framework.domains.booking.models.Theater;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * TheaterApiClient - API operations for theaters
 *
 * Used for:
 * - Theater lookups
 * - Show time queries by theater
 * - Location-based searches
 */
@Slf4j
@Component
public class TheaterApiClient extends BaseApiClient {

    @Override
    protected String getBasePath() {
        return "/api/v1/theaters";
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // THEATER QUERIES
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Gets all theaters in a city.
     */
    public List<Theater> getTheatersByCity(String city) {
        log.info("Getting theaters in city: {}", city);
        Response response = get("", Map.of("city", city));
        return List.of(getOkAs(response, Theater[].class));
    }

    /**
     * Gets theaters near a location.
     */
    public List<Theater> getNearbyTheaters(double latitude, double longitude, int radiusKm) {
        log.info("Getting theaters near: {}, {} within {} km", latitude, longitude, radiusKm);
        Response response = get("/nearby", Map.of(
            "lat", latitude,
            "lng", longitude,
            "radius", radiusKm
        ));
        return List.of(getOkAs(response, Theater[].class));
    }

    /**
     * Gets theater by ID.
     */
    public Theater getTheater(String theaterId) {
        log.info("Getting theater: {}", theaterId);
        Response response = get("/" + theaterId);
        return getOkAs(response, Theater.class);
    }

    /**
     * Searches theaters by name.
     */
    public List<Theater> searchTheaters(String query) {
        log.info("Searching theaters: {}", query);
        Response response = get("/search", Map.of("q", query));
        return List.of(getOkAs(response, Theater[].class));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SHOW TIMES BY THEATER
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Gets all show times at a theater for a date.
     */
    public List<ShowTime> getShowTimes(String theaterId, LocalDate date) {
        log.info("Getting show times at theater: {} on date: {}", theaterId, date);
        Response response = get("/" + theaterId + "/showtimes", Map.of(
            "date", date.toString()
        ));
        return List.of(getOkAs(response, ShowTime[].class));
    }

    /**
     * Gets show times for a specific movie at a theater.
     */
    public List<ShowTime> getShowTimesForMovie(String theaterId, String movieId, LocalDate date) {
        log.info("Getting show times for movie: {} at theater: {} on date: {}", movieId, theaterId, date);
        Response response = get("/" + theaterId + "/showtimes", Map.of(
            "date", date.toString(),
            "movieId", movieId
        ));
        return List.of(getOkAs(response, ShowTime[].class));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // THEATER FEATURES
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Gets theaters with specific facilities.
     */
    public List<Theater> getTheatersWithFacility(String city, String facility) {
        log.info("Getting theaters in {} with facility: {}", city, facility);
        Response response = get("", Map.of(
            "city", city,
            "facility", facility
        ));
        return List.of(getOkAs(response, Theater[].class));
    }

    /**
     * Gets theaters showing a specific format (IMAX, 4DX, etc.)
     */
    public List<Theater> getTheatersWithFormat(String city, String format) {
        log.info("Getting theaters in {} with format: {}", city, format);
        Response response = get("", Map.of(
            "city", city,
            "format", format
        ));
        return List.of(getOkAs(response, Theater[].class));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TEST DATA HELPERS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Gets first theater in a city.
     */
    public Theater getFirstTheaterInCity(String city) {
        List<Theater> theaters = getTheatersByCity(city);
        return theaters.isEmpty() ? null : theaters.get(0);
    }

    /**
     * Gets first theater with available shows for a movie.
     */
    public Theater getTheaterWithShowsForMovie(String city, String movieId, LocalDate date) {
        List<Theater> theaters = getTheatersByCity(city);
        for (Theater theater : theaters) {
            List<ShowTime> showTimes = getShowTimesForMovie(theater.getId(), movieId, date);
            if (!showTimes.isEmpty() && showTimes.stream().anyMatch(ShowTime::hasAvailability)) {
                return theater;
            }
        }
        return null;
    }
}
