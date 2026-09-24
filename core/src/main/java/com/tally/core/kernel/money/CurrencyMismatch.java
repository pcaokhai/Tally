package com.tally.core.kernel.money;

import java.util.Currency;

/** Thrown when a {@link Money} operation combines two amounts in different currencies. */
public class CurrencyMismatch extends IllegalArgumentException {

    public CurrencyMismatch(Currency expected, Currency actual) {
        super("cannot combine %s with %s".formatted(expected.getCurrencyCode(), actual.getCurrencyCode()));
    }
}
