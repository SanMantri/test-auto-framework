package com.company.automation.tests.payment;

import com.company.automation.playbooks.CheckoutPlaybook;
import com.company.automation.tests.BaseTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.Assert;
import org.testng.annotations.Test;

public class PaymentTests extends BaseTest {

    @Autowired
    private CheckoutPlaybook checkoutPlaybook;

    @Test(groups = { "payment", "regression" })
    public void verifyCreditCardPurchase() {
        // 1. Setup: Use API to add item to Cart (Simulated)
        // apiClient.addToCart("Pixel 9 Pro");

        page.navigate("https://my-demo-shop.com/checkout");

        // 2. Action: Use Playbook to handle the UI flow
        checkoutPlaybook.attemptCreditCardPurchase(page, "4111222233334444", "123");

        // 3. Verify
        Assert.assertTrue(page.locator("text=Order Placed Successfully").isVisible(),
                "Order success message should be visible");
    }
}
