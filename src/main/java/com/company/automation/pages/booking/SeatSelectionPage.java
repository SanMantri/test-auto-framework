package com.company.automation.pages.booking;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.Locator;

import java.util.List;

public class SeatSelectionPage {
    private final Page page;

    // A list of available seats
    private final String availableSeatParams = ".seat.available";
    private final String proceedBtn = "#proceed-to-pay";

    public SeatSelectionPage(Page page) {
        this.page = page;
    }

    public void selectSeats(int count) {
        Locator availableSeats = page.locator(availableSeatParams);
        int found = availableSeats.count();

        if (found < count) {
            throw new RuntimeException("Not enough seats available!");
        }

        // Select 'count' seats
        for (int i = 0; i < count; i++) {
            availableSeats.nth(i).click();
        }
    }

    public void proceed() {
        page.locator(proceedBtn).click();
    }
}
