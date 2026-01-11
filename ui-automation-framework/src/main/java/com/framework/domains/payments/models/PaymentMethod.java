package com.framework.domains.payments.models;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * PaymentMethod - Supported payment methods
 */
@Getter
@RequiredArgsConstructor
public enum PaymentMethod {

    CREDIT_CARD("credit_card", "Credit Card"),
    DEBIT_CARD("debit_card", "Debit Card"),
    UPI("upi", "UPI"),
    NET_BANKING("net_banking", "Net Banking"),
    WALLET("wallet", "Wallet"),
    EMI("emi", "EMI"),
    COD("cod", "Cash on Delivery");

    private final String value;
    private final String displayName;

    public static PaymentMethod fromValue(String value) {
        for (PaymentMethod method : values()) {
            if (method.value.equalsIgnoreCase(value)) {
                return method;
            }
        }
        throw new IllegalArgumentException("Unknown payment method: " + value);
    }
}
