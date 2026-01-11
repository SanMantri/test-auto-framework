package com.framework.domains.booking.pages;

import com.framework.core.base.BasePage;
import com.microsoft.playwright.Page;
import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * MovieListingPage - Movie listing and search page
 *
 * Handles:
 * - Now showing / Coming soon listings
 * - Movie search
 * - Filtering by genre, language, format
 * - Movie card interactions
 */
@Slf4j
public class MovieListingPage extends BasePage {

    // ═══════════════════════════════════════════════════════════════════════════
    // LOCATORS
    // ═══════════════════════════════════════════════════════════════════════════

    // Navigation Tabs
    private static final String NOW_SHOWING_TAB = "[data-testid='now-showing-tab'], a:has-text('Now Showing')";
    private static final String COMING_SOON_TAB = "[data-testid='coming-soon-tab'], a:has-text('Coming Soon')";
    private static final String ACTIVE_TAB = "[data-testid='active-tab'], .tab-active";

    // Search
    private static final String SEARCH_INPUT = "[data-testid='movie-search'], input[placeholder*='Search']";
    private static final String SEARCH_RESULTS = "[data-testid='search-results']";
    private static final String SEARCH_RESULT_ITEM = "[data-testid='search-result-item']";

    // Filters
    private static final String FILTER_PANEL = "[data-testid='filter-panel']";
    private static final String GENRE_FILTER = "[data-testid='genre-filter-%s']";
    private static final String LANGUAGE_FILTER = "[data-testid='language-filter-%s']";
    private static final String FORMAT_FILTER = "[data-testid='format-filter-%s']";
    private static final String CLEAR_FILTERS = "[data-testid='clear-filters']";
    private static final String ACTIVE_FILTER = "[data-testid='active-filter']";

    // Movie Cards
    private static final String MOVIE_CARD = "[data-testid='movie-card']";
    private static final String MOVIE_CARD_BY_ID = "[data-testid='movie-card-%s']";
    private static final String MOVIE_TITLE = "[data-testid='movie-title']";
    private static final String MOVIE_POSTER = "[data-testid='movie-poster']";
    private static final String MOVIE_RATING = "[data-testid='movie-rating']";
    private static final String MOVIE_GENRE = "[data-testid='movie-genre']";
    private static final String MOVIE_LANGUAGE = "[data-testid='movie-language']";
    private static final String BOOK_NOW_BUTTON = "[data-testid='book-now-%s'], button:has-text('Book')";

    // City Selection
    private static final String CITY_SELECTOR = "[data-testid='city-selector']";
    private static final String CITY_OPTION = "[data-testid='city-%s']";
    private static final String CURRENT_CITY = "[data-testid='current-city']";

    // Loading
    private static final String LOADING_SKELETON = "[data-testid='loading-skeleton']";
    private static final String NO_MOVIES = "[data-testid='no-movies'], .no-results";

    // ═══════════════════════════════════════════════════════════════════════════
    // CONSTRUCTOR & NAVIGATION
    // ═══════════════════════════════════════════════════════════════════════════

    public MovieListingPage(Page page) {
        super(page);
    }

    @Step("Navigate to movie listing page")
    public MovieListingPage navigate() {
        navigateTo("/movies");
        return this;
    }

    @Override
    protected void waitForPageLoad() {
        waitForHidden(LOADING_SKELETON);
        waitForVisible(MOVIE_CARD);
    }

    @Override
    public boolean isDisplayed() {
        return isVisible(NOW_SHOWING_TAB) || isVisible(COMING_SOON_TAB);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CITY SELECTION
    // ═══════════════════════════════════════════════════════════════════════════

    @Step("Select city: {city}")
    public MovieListingPage selectCity(String city) {
        log.info("Selecting city: {}", city);

        click(CITY_SELECTOR);
        click(String.format(CITY_OPTION, city.toLowerCase()));

        // Wait for movies to reload
        waitForHidden(LOADING_SKELETON);

        return this;
    }

    public String getCurrentCity() {
        return getText(CURRENT_CITY);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TAB NAVIGATION
    // ═══════════════════════════════════════════════════════════════════════════

    @Step("Click Now Showing tab")
    public MovieListingPage clickNowShowing() {
        log.info("Clicking Now Showing tab");
        click(NOW_SHOWING_TAB);
        waitForHidden(LOADING_SKELETON);
        return this;
    }

    @Step("Click Coming Soon tab")
    public MovieListingPage clickComingSoon() {
        log.info("Clicking Coming Soon tab");
        click(COMING_SOON_TAB);
        waitForHidden(LOADING_SKELETON);
        return this;
    }

    public String getActiveTab() {
        return getAttribute(ACTIVE_TAB, "data-tab");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SEARCH
    // ═══════════════════════════════════════════════════════════════════════════

    @Step("Search for movie: {query}")
    public MovieListingPage searchMovie(String query) {
        log.info("Searching for movie: {}", query);

        fill(SEARCH_INPUT, query);
        waitForVisible(SEARCH_RESULTS);

        return this;
    }

    public List<String> getSearchResults() {
        return page.locator(SEARCH_RESULT_ITEM)
            .all()
            .stream()
            .map(el -> el.textContent())
            .toList();
    }

    @Step("Select search result: {movieTitle}")
    public MovieDetailsPage selectSearchResult(String movieTitle) {
        log.info("Selecting search result: {}", movieTitle);

        page.locator(SEARCH_RESULT_ITEM + ":has-text('" + movieTitle + "')").click();

        return new MovieDetailsPage(page);
    }

    public void clearSearch() {
        fill(SEARCH_INPUT, "");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // FILTERS
    // ═══════════════════════════════════════════════════════════════════════════

    @Step("Filter by genre: {genre}")
    public MovieListingPage filterByGenre(String genre) {
        log.info("Filtering by genre: {}", genre);
        click(String.format(GENRE_FILTER, genre.toLowerCase()));
        waitForHidden(LOADING_SKELETON);
        return this;
    }

    @Step("Filter by language: {language}")
    public MovieListingPage filterByLanguage(String language) {
        log.info("Filtering by language: {}", language);
        click(String.format(LANGUAGE_FILTER, language.toLowerCase()));
        waitForHidden(LOADING_SKELETON);
        return this;
    }

    @Step("Filter by format: {format}")
    public MovieListingPage filterByFormat(String format) {
        log.info("Filtering by format: {}", format);
        click(String.format(FORMAT_FILTER, format.toLowerCase()));
        waitForHidden(LOADING_SKELETON);
        return this;
    }

    @Step("Clear all filters")
    public MovieListingPage clearFilters() {
        log.info("Clearing all filters");
        click(CLEAR_FILTERS);
        waitForHidden(LOADING_SKELETON);
        return this;
    }

    public List<String> getActiveFilters() {
        return page.locator(ACTIVE_FILTER)
            .all()
            .stream()
            .map(el -> el.textContent())
            .toList();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // MOVIE CARDS
    // ═══════════════════════════════════════════════════════════════════════════

    public int getMovieCount() {
        return page.locator(MOVIE_CARD).count();
    }

    public boolean hasNoMovies() {
        return isVisible(NO_MOVIES);
    }

    public List<String> getMovieTitles() {
        return page.locator(MOVIE_CARD + " " + MOVIE_TITLE)
            .all()
            .stream()
            .map(el -> el.textContent())
            .toList();
    }

    @Step("Click on movie card: {movieId}")
    public MovieDetailsPage clickMovieCard(String movieId) {
        log.info("Clicking movie card: {}", movieId);
        click(String.format(MOVIE_CARD_BY_ID, movieId));
        return new MovieDetailsPage(page);
    }

    @Step("Click Book Now for movie: {movieId}")
    public ShowTimesPage clickBookNow(String movieId) {
        log.info("Clicking Book Now for movie: {}", movieId);
        click(String.format(BOOK_NOW_BUTTON, movieId));
        return new ShowTimesPage(page);
    }

    public String getMovieRating(String movieId) {
        String locator = String.format(MOVIE_CARD_BY_ID, movieId) + " " + MOVIE_RATING;
        return getText(locator);
    }

    public String getMovieGenre(String movieId) {
        String locator = String.format(MOVIE_CARD_BY_ID, movieId) + " " + MOVIE_GENRE;
        return getText(locator);
    }

    public boolean isMovieDisplayed(String movieId) {
        return isVisible(String.format(MOVIE_CARD_BY_ID, movieId));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // QUICK ACTIONS
    // ═══════════════════════════════════════════════════════════════════════════

    @Step("Navigate to first available movie")
    public MovieDetailsPage goToFirstMovie() {
        page.locator(MOVIE_CARD).first().click();
        return new MovieDetailsPage(page);
    }

    @Step("Book first available movie")
    public ShowTimesPage bookFirstMovie() {
        page.locator(MOVIE_CARD + " button:has-text('Book')").first().click();
        return new ShowTimesPage(page);
    }
}
