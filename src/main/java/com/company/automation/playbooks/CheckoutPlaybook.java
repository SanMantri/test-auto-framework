package com.company.automation.playbooks;

import com.company.automation.pages.payment.PaymentPage;
import com.microsoft.playwright.Page;
import org.springframework.stereotype.Component;

/**
 * Playbooks encapsulate User Journeys.
 * They orchestrate multiple Page Objects to achieve a Business Goal.
 */
@Component
public class CheckoutPlaybook {

    public void attemptCreditCardPurchase(Page page, String cardNum, String cvv) {
        PaymentPage paymentPage = new PaymentPage(page);

        System.out.println("Playbook: Starting Credit Card Purchase...");
        paymentPage.selectCreditCard(); // Step 1
        paymentPage.enterCardDetails(cardNum, cvv); // Step 2
        paymentPage.placeOrder(); // Step 3

        // Validation could be here or in the test
    }
}
