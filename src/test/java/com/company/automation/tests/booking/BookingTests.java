package com.company.automation.tests.booking;

import com.company.automation.pages.booking.SeatSelectionPage;
import com.company.automation.tests.BaseTest;
import org.testng.Assert;
import org.testng.annotations.Test;

public class BookingTests extends BaseTest {

    @Test(groups = { "booking" })
    public void bookMovieTickets() {
        // 1. SMART SETUP: Use API to find a showID that HAS seats
        // String showId = movieApiClient.findShowWithAvailability("Inception");
        String showId = "show_12345"; // Mock

        // 2. Direct Navigation (No UI Search)
        page.navigate("https://bookmyshow-demo.com/buy/" + showId);

        // 3. Select Seats via POM
        SeatSelectionPage selectionPage = new SeatSelectionPage(page);
        selectionPage.selectSeats(2);
        selectionPage.proceed();

        // 4. Assertion
        Assert.assertTrue(page.url().contains("payment"), "Should be navigated to payment page");
    }
}
