package com.framework.domains.booking.api;

import com.framework.core.base.BaseApiClient;
import com.framework.domains.booking.models.Movie;
import com.framework.domains.booking.models.ShowTime;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * MovieApiClient - API operations for movies
 *
 * Used for:
 * - Fetching movie listings
 * - Getting show times
 * - Test data setup
 */
@Slf4j
@Component
public class MovieApiClient extends BaseApiClient {

    @Override
    protected String getBasePath() {
        return "/api/v1/movies";
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // MOVIE QUERIES
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Gets all now showing movies.
     */
    public List<Movie> getNowShowing() {
        log.info("Getting now showing movies");
        Response response = get("/now-showing");
        return List.of(getOkAs(response, Movie[].class));
    }

    /**
     * Gets all coming soon movies.
     */
    public List<Movie> getComingSoon() {
        log.info("Getting coming soon movies");
        Response response = get("/coming-soon");
        return List.of(getOkAs(response, Movie[].class));
    }

    /**
     * Gets movie by ID.
     */
    public Movie getMovie(String movieId) {
        log.info("Getting movie: {}", movieId);
        Response response = get("/" + movieId);
        return getOkAs(response, Movie.class);
    }

    /**
     * Searches movies by title.
     */
    public List<Movie> searchMovies(String query) {
        log.info("Searching movies: {}", query);
        Response response = get("/search", Map.of("q", query));
        return List.of(getOkAs(response, Movie[].class));
    }

    /**
     * Gets movies filtered by genre.
     */
    public List<Movie> getMoviesByGenre(String genre) {
        log.info("Getting movies by genre: {}", genre);
        Response response = get("", Map.of("genre", genre));
        return List.of(getOkAs(response, Movie[].class));
    }

    /**
     * Gets movies filtered by language.
     */
    public List<Movie> getMoviesByLanguage(String language) {
        log.info("Getting movies by language: {}", language);
        Response response = get("", Map.of("language", language));
        return List.of(getOkAs(response, Movie[].class));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SHOW TIMES
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Gets show times for a movie on a specific date.
     */
    public List<ShowTime> getShowTimes(String movieId, LocalDate date) {
        log.info("Getting show times for movie: {} on date: {}", movieId, date);
        Response response = get("/" + movieId + "/showtimes", Map.of(
            "date", date.toString()
        ));
        return List.of(getOkAs(response, ShowTime[].class));
    }

    /**
     * Gets show times for a movie at a specific theater.
     */
    public List<ShowTime> getShowTimes(String movieId, String theaterId, LocalDate date) {
        log.info("Getting show times for movie: {} at theater: {} on date: {}", movieId, theaterId, date);
        Response response = get("/" + movieId + "/showtimes", Map.of(
            "date", date.toString(),
            "theaterId", theaterId
        ));
        return List.of(getOkAs(response, ShowTime[].class));
    }

    /**
     * Gets show times filtered by format (2D, 3D, IMAX).
     */
    public List<ShowTime> getShowTimesByFormat(String movieId, LocalDate date, String format) {
        log.info("Getting {} show times for movie: {} on date: {}", format, movieId, date);
        Response response = get("/" + movieId + "/showtimes", Map.of(
            "date", date.toString(),
            "format", format
        ));
        return List.of(getOkAs(response, ShowTime[].class));
    }

    /**
     * Gets a specific show time.
     */
    public ShowTime getShowTime(String showTimeId) {
        log.info("Getting show time: {}", showTimeId);
        Response response = get("/showtimes/" + showTimeId);
        return getOkAs(response, ShowTime.class);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TEST DATA HELPERS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Gets first available movie with show times.
     */
    public Movie getFirstAvailableMovie() {
        List<Movie> movies = getNowShowing();
        return movies.isEmpty() ? null : movies.get(0);
    }

    /**
     * Gets first available show time for a movie.
     */
    public ShowTime getFirstAvailableShowTime(String movieId) {
        List<ShowTime> showTimes = getShowTimes(movieId, LocalDate.now());
        return showTimes.stream()
            .filter(ShowTime::hasAvailability)
            .findFirst()
            .orElse(null);
    }
}
