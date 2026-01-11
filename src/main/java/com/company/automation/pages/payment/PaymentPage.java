package com.company.automation.pages.payment;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.Locator;

public class PaymentPage {
    private final Page page;

    // Selectors
    private final String creditCardRadio = "input[name='paymentMethod'][value='CC']";
    private final String cardNumberInput = "#cc-number"; // Often inside an iframe in real apps
    private final String cvvInput = "#cc-cvv";
    private final String placeOrderBtn = "#place-order-button";

    public PaymentPage(Page page) {
        this.page = page;
    }

    public void selectCreditCard() {
        page.locator(creditCardRadio).click();
    }

    public void enterCardDetails(String number, String cvv) {
        // In a real Amazon-like app, this might be a Frame
        // page.frameLocator("#payment-frame").locator(cardNumberInput).fill(number);

        page.locator(cardNumberInput).fill(number);
        page.locator(cvvInput).fill(cvv);
    }

    public void placeOrder() {
        page.locator(placeOrderBtn).click();
    }

    public boolean isOrderSuccess() {
        return page.locator("text=Order Placed Successfully").isVisible();
    }
}
