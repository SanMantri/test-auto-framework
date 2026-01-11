package com.framework.domains.booking.pages;

import com.framework.core.base.BasePage;
import com.microsoft.playwright.Page;
import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;

/**
 * ShowTimesPage - Dedicated show times selection page
 *
 * Alternative entry point when user clicks "Book Tickets" directly.
 */
@Slf4j
public class ShowTimesPage extends BasePage {

    // ═══════════════════════════════════════════════════════════════════════════
    // LOCATORS
    // ═══════════════════════════════════════════════════════════════════════════

    private static final String PAGE_TITLE = "[data-testid='showtimes-title']";
    private static final String MOVIE_INFO = "[data-testid='movie-info']";
    private static final String DATE_SELECTOR = "[data-testid='date-selector']";
    private static final String DATE_OPTION = "[data-testid='date-%s']";
    private static final String THEATER_LIST = "[data-testid='theater-list']";
    private static final String SHOW_TIME = "[data-testid='showtime-%s']";
    private static final String LOADING = "[data-testid='loading']";

    // ═══════════════════════════════════════════════════════════════════════════
    // CONSTRUCTOR
    // ═══════════════════════════════════════════════════════════════════════════

    public ShowTimesPage(Page page) {
        super(page);
    }

    @Override
    protected void waitForPageLoad() {
        waitForHidden(LOADING);
        waitForVisible(THEATER_LIST);
    }

    @Override
    public boolean isDisplayed() {
        return isVisible(THEATER_LIST);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ACTIONS
    // ═══════════════════════════════════════════════════════════════════════════

    @Step("Select date: {date}")
    public ShowTimesPage selectDate(String date) {
        log.info("Selecting date: {}", date);
        click(String.format(DATE_OPTION, date));
        waitForHidden(LOADING);
        return this;
    }

    @Step("Select show time: {showTimeId}")
    public SeatSelectionPage selectShowTime(String showTimeId) {
        log.info("Selecting show time: {}", showTimeId);
        click(String.format(SHOW_TIME, showTimeId));
        return new SeatSelectionPage(page);
    }

    public String getMovieInfo() {
        return getText(MOVIE_INFO);
    }
}
