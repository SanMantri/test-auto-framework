package com.framework.domains.payments.models;

import lombok.Builder;
import lombok.Data;

/**
 * TestCard - Test credit/debit card data
 *
 * Standard test card numbers for payment gateway testing.
 * These are industry-standard test cards that trigger specific behaviors.
 */
@Data
@Builder
public class TestCard {

    private String number;
    private String expiry;
    private String cvv;
    private String name;
    private ExpectedResult expectedResult;
    private String otpCode;

    public enum ExpectedResult {
        SUCCESS,
        DECLINED,
        REQUIRES_3DS,
        INSUFFICIENT_FUNDS,
        EXPIRED,
        INVALID_CVV,
        PROCESSING_ERROR
    }

    public boolean requires3DS() {
        return expectedResult == ExpectedResult.REQUIRES_3DS;
    }

    public boolean shouldSucceed() {
        return expectedResult == ExpectedResult.SUCCESS;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PRE-DEFINED TEST CARDS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Visa card that always succeeds
     */
    public static final TestCard VISA_SUCCESS = TestCard.builder()
        .number("4111111111111111")
        .expiry("12/28")
        .cvv("123")
        .name("Test User")
        .expectedResult(ExpectedResult.SUCCESS)
        .build();

    /**
     * Visa card that gets declined
     */
    public static final TestCard VISA_DECLINED = TestCard.builder()
        .number("4000000000000002")
        .expiry("12/28")
        .cvv("123")
        .name("Test User")
        .expectedResult(ExpectedResult.DECLINED)
        .build();

    /**
     * Visa card that requires 3DS authentication
     */
    public static final TestCard VISA_3DS = TestCard.builder()
        .number("4000000000003220")
        .expiry("12/28")
        .cvv("123")
        .name("Test User")
        .expectedResult(ExpectedResult.REQUIRES_3DS)
        .otpCode("123456")
        .build();

    /**
     * Visa card with insufficient funds
     */
    public static final TestCard VISA_INSUFFICIENT_FUNDS = TestCard.builder()
        .number("4000000000009995")
        .expiry("12/28")
        .cvv("123")
        .name("Test User")
        .expectedResult(ExpectedResult.INSUFFICIENT_FUNDS)
        .build();

    /**
     * Mastercard that always succeeds
     */
    public static final TestCard MASTERCARD_SUCCESS = TestCard.builder()
        .number("5555555555554444")
        .expiry("12/28")
        .cvv("123")
        .name("Test User")
        .expectedResult(ExpectedResult.SUCCESS)
        .build();

    /**
     * Mastercard that requires 3DS
     */
    public static final TestCard MASTERCARD_3DS = TestCard.builder()
        .number("5200000000001096")
        .expiry("12/28")
        .cvv("123")
        .name("Test User")
        .expectedResult(ExpectedResult.REQUIRES_3DS)
        .otpCode("123456")
        .build();

    /**
     * AMEX that always succeeds
     */
    public static final TestCard AMEX_SUCCESS = TestCard.builder()
        .number("378282246310005")
        .expiry("12/28")
        .cvv("1234")  // AMEX uses 4-digit CVV
        .name("Test User")
        .expectedResult(ExpectedResult.SUCCESS)
        .build();

    /**
     * Card with expired date
     */
    public static final TestCard EXPIRED_CARD = TestCard.builder()
        .number("4111111111111111")
        .expiry("12/20")  // Past date
        .cvv("123")
        .name("Test User")
        .expectedResult(ExpectedResult.EXPIRED)
        .build();

    /**
     * Card with invalid CVV
     */
    public static final TestCard INVALID_CVV = TestCard.builder()
        .number("4000000000000101")
        .expiry("12/28")
        .cvv("999")
        .name("Test User")
        .expectedResult(ExpectedResult.INVALID_CVV)
        .build();
}
