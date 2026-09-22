package com.tally.core.kernel.money;

import java.util.Currency;
import java.util.Objects;

/**
 * An amount in minor units of a single ISO currency (ADR-004). Arithmetic overflows rather than
 * wrapping, and mixing currencies is a programming error.
 */
public record Money(long minor, Currency currency) {

    public Money {
        Objects.requireNonNull(currency, "currency");
    }

    public Money plus(Money other) {
        return new Money(Math.addExact(minor, sameCurrency(other).minor()), currency);
    }

    public Money minus(Money other) {
        return new Money(Math.subtractExact(minor, sameCurrency(other).minor()), currency);
    }

    private Money sameCurrency(Money other) {
        if (!currency.equals(other.currency())) {
            throw new IllegalArgumentException("cannot combine %s with %s"
                    .formatted(currency.getCurrencyCode(), other.currency().getCurrencyCode()));
        }
        return other;
    }
}
