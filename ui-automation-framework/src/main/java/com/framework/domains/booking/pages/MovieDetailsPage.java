package com.framework.domains.booking.pages;

import com.framework.core.base.BasePage;
import com.microsoft.playwright.Page;
import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * MovieDetailsPage - Movie details and show time selection
 *
 * Handles:
 * - Movie information display
 * - Date selection
 * - Format/Language selection
 * - Theater and show time listing
 */
@Slf4j
public class MovieDetailsPage extends BasePage {

    // ═══════════════════════════════════════════════════════════════════════════
    // LOCATORS
    // ═══════════════════════════════════════════════════════════════════════════

    // Movie Info
    private static final String MOVIE_TITLE = "[data-testid='movie-title'], h1.movie-title";
    private static final String MOVIE_POSTER = "[data-testid='movie-poster']";
    private static final String MOVIE_DESCRIPTION = "[data-testid='movie-description']";
    private static final String MOVIE_DURATION = "[data-testid='movie-duration']";
    private static final String MOVIE_GENRE = "[data-testid='movie-genre']";
    private static final String MOVIE_RELEASE_DATE = "[data-testid='release-date']";
    private static final String MOVIE_RATING = "[data-testid='movie-rating']";
    private static final String USER_RATING = "[data-testid='user-rating']";
    private static final String TOTAL_REVIEWS = "[data-testid='total-reviews']";

    // Cast & Crew
    private static final String CAST_MEMBER = "[data-testid='cast-member']";
    private static final String DIRECTOR = "[data-testid='director']";

    // Trailer
    private static final String PLAY_TRAILER = "[data-testid='play-trailer'], button:has-text('Trailer')";
    private static final String TRAILER_MODAL = "[data-testid='trailer-modal']";
    private static final String CLOSE_TRAILER = "[data-testid='close-trailer']";

    // Date Selection
    private static final String DATE_PICKER = "[data-testid='date-picker']";
    private static final String DATE_OPTION = "[data-testid='date-%s']";
    private static final String SELECTED_DATE = "[data-testid='selected-date']";

    // Format/Language Selection
    private static final String FORMAT_SELECTOR = "[data-testid='format-selector']";
    private static final String FORMAT_OPTION = "[data-testid='format-%s']";
    private static final String LANGUAGE_SELECTOR = "[data-testid='language-selector']";
    private static final String LANGUAGE_OPTION = "[data-testid='language-%s']";

    // Theater Listings
    private static final String THEATER_LISTING = "[data-testid='theater-listing']";
    private static final String THEATER_CARD = "[data-testid='theater-card']";
    private static final String THEATER_CARD_BY_ID = "[data-testid='theater-%s']";
    private static final String THEATER_NAME = "[data-testid='theater-name']";
    private static final String THEATER_FACILITIES = "[data-testid='theater-facilities']";

    // Show Times
    private static final String SHOW_TIME_BUTTON = "[data-testid='showtime-%s']";
    private static final String SHOW_TIME_AVAILABLE = "[data-testid='showtime'].available";
    private static final String SHOW_TIME_FAST_FILLING = "[data-testid='showtime'].fast-filling";
    private static final String SHOW_TIME_SOLD_OUT = "[data-testid='showtime'].sold-out";

    // Book Tickets
    private static final String BOOK_TICKETS_BUTTON = "[data-testid='book-tickets'], button:has-text('Book Tickets')";

    // Loading
    private static final String LOADING = "[data-testid='loading']";
    private static final String NO_SHOWS = "[data-testid='no-shows']";

    // ═══════════════════════════════════════════════════════════════════════════
    // CONSTRUCTOR & NAVIGATION
    // ═══════════════════════════════════════════════════════════════════════════

    public MovieDetailsPage(Page page) {
        super(page);
    }

    @Step("Navigate to movie details: {movieId}")
    public MovieDetailsPage navigate(String movieId) {
        navigateTo("/movies/" + movieId);
        return this;
    }

    @Override
    protected void waitForPageLoad() {
        waitForVisible(MOVIE_TITLE);
    }

    @Override
    public boolean isDisplayed() {
        return isVisible(MOVIE_TITLE);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // MOVIE INFORMATION
    // ═══════════════════════════════════════════════════════════════════════════

    public String getMovieTitle() {
        return getText(MOVIE_TITLE);
    }

    public String getDescription() {
        return getText(MOVIE_DESCRIPTION);
    }

    public String getDuration() {
        return getText(MOVIE_DURATION);
    }

    public String getGenre() {
        return getText(MOVIE_GENRE);
    }

    public String getReleaseDate() {
        return getText(MOVIE_RELEASE_DATE);
    }

    public String getCertification() {
        return getText(MOVIE_RATING);
    }

    public String getUserRating() {
        return getText(USER_RATING);
    }

    public String getTotalReviews() {
        return getText(TOTAL_REVIEWS);
    }

    public String getDirector() {
        return getText(DIRECTOR);
    }

    public List<String> getCastMembers() {
        return page.locator(CAST_MEMBER)
            .all()
            .stream()
            .map(el -> el.textContent())
            .toList();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TRAILER
    // ═══════════════════════════════════════════════════════════════════════════

    @Step("Play trailer")
    public MovieDetailsPage playTrailer() {
        log.info("Playing trailer");
        click(PLAY_TRAILER);
        waitForVisible(TRAILER_MODAL);
        return this;
    }

    @Step("Close trailer")
    public MovieDetailsPage closeTrailer() {
        log.info("Closing trailer");
        click(CLOSE_TRAILER);
        waitForHidden(TRAILER_MODAL);
        return this;
    }

    public boolean isTrailerModalVisible() {
        return isVisible(TRAILER_MODAL);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DATE SELECTION
    // ═══════════════════════════════════════════════════════════════════════════

    @Step("Select date: {date}")
    public MovieDetailsPage selectDate(String date) {
        log.info("Selecting date: {}", date);
        click(String.format(DATE_OPTION, date));
        waitForHidden(LOADING);
        return this;
    }

    public String getSelectedDate() {
        return getAttribute(SELECTED_DATE, "data-date");
    }

    public List<String> getAvailableDates() {
        return page.locator(DATE_PICKER + " [data-testid^='date-']")
            .all()
            .stream()
            .map(el -> el.getAttribute("data-date"))
            .toList();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // FORMAT & LANGUAGE SELECTION
    // ═══════════════════════════════════════════════════════════════════════════

    @Step("Select format: {format}")
    public MovieDetailsPage selectFormat(String format) {
        log.info("Selecting format: {}", format);
        click(String.format(FORMAT_OPTION, format.toLowerCase()));
        waitForHidden(LOADING);
        return this;
    }

    @Step("Select language: {language}")
    public MovieDetailsPage selectLanguage(String language) {
        log.info("Selecting language: {}", language);
        click(String.format(LANGUAGE_OPTION, language.toLowerCase()));
        waitForHidden(LOADING);
        return this;
    }

    public List<String> getAvailableFormats() {
        return page.locator(FORMAT_SELECTOR + " [data-testid^='format-']")
            .all()
            .stream()
            .map(el -> el.getAttribute("data-format"))
            .toList();
    }

    public List<String> getAvailableLanguages() {
        return page.locator(LANGUAGE_SELECTOR + " [data-testid^='language-']")
            .all()
            .stream()
            .map(el -> el.getAttribute("data-language"))
            .toList();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // THEATER LISTINGS
    // ═══════════════════════════════════════════════════════════════════════════

    public int getTheaterCount() {
        return page.locator(THEATER_CARD).count();
    }

    public boolean hasNoShows() {
        return isVisible(NO_SHOWS);
    }

    public List<String> getTheaterNames() {
        return page.locator(THEATER_CARD + " " + THEATER_NAME)
            .all()
            .stream()
            .map(el -> el.textContent())
            .toList();
    }

    public String getTheaterFacilities(String theaterId) {
        String locator = String.format(THEATER_CARD_BY_ID, theaterId) + " " + THEATER_FACILITIES;
        return getText(locator);
    }

    public boolean isTheaterDisplayed(String theaterId) {
        return isVisible(String.format(THEATER_CARD_BY_ID, theaterId));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SHOW TIME SELECTION
    // ═══════════════════════════════════════════════════════════════════════════

    @Step("Select show time: {showTimeId}")
    public SeatSelectionPage selectShowTime(String showTimeId) {
        log.info("Selecting show time: {}", showTimeId);
        click(String.format(SHOW_TIME_BUTTON, showTimeId));
        return new SeatSelectionPage(page);
    }

    public List<String> getShowTimesForTheater(String theaterId) {
        String locator = String.format(THEATER_CARD_BY_ID, theaterId) + " [data-testid^='showtime-']";
        return page.locator(locator)
            .all()
            .stream()
            .map(el -> el.getAttribute("data-showtime-id"))
            .toList();
    }

    public int getAvailableShowTimeCount() {
        return page.locator(SHOW_TIME_AVAILABLE).count();
    }

    public int getFastFillingShowTimeCount() {
        return page.locator(SHOW_TIME_FAST_FILLING).count();
    }

    public int getSoldOutShowTimeCount() {
        return page.locator(SHOW_TIME_SOLD_OUT).count();
    }

    public boolean isShowTimeSoldOut(String showTimeId) {
        String locator = String.format(SHOW_TIME_BUTTON, showTimeId);
        return page.locator(locator).getAttribute("class").contains("sold-out");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // QUICK ACTIONS
    // ═══════════════════════════════════════════════════════════════════════════

    @Step("Click Book Tickets button")
    public ShowTimesPage clickBookTickets() {
        log.info("Clicking Book Tickets button");
        click(BOOK_TICKETS_BUTTON);
        return new ShowTimesPage(page);
    }

    @Step("Select first available show time")
    public SeatSelectionPage selectFirstAvailableShowTime() {
        log.info("Selecting first available show time");
        page.locator(SHOW_TIME_AVAILABLE).first().click();
        return new SeatSelectionPage(page);
    }

    @Step("Select first available show time at theater: {theaterId}")
    public SeatSelectionPage selectFirstShowTimeAtTheater(String theaterId) {
        log.info("Selecting first available show time at theater: {}", theaterId);
        String locator = String.format(THEATER_CARD_BY_ID, theaterId) + " " + SHOW_TIME_AVAILABLE;
        page.locator(locator).first().click();
        return new SeatSelectionPage(page);
    }
}
