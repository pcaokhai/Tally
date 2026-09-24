package com.tally.core.kernel.money;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Currency;
import java.util.List;
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

    public Money times(long factor) {
        return new Money(Math.multiplyExact(minor, factor), currency);
    }

    public Money negate() {
        return new Money(Math.negateExact(minor), currency);
    }

    public boolean isZero() {
        return minor == 0;
    }

    /**
     * Splits this amount proportionally to {@code ratios} using the largest-remainder method, so
     * the parts always sum back to exactly this amount (TLY-201-AC4).
     */
    public List<Money> allocate(long... ratios) {
        if (ratios.length == 0) {
            throw new IllegalArgumentException("ratios must not be empty");
        }
        long totalRatio = 0;
        for (long ratio : ratios) {
            if (ratio < 0) {
                throw new IllegalArgumentException("ratio must be non-negative: " + ratio);
            }
            totalRatio = Math.addExact(totalRatio, ratio);
        }
        if (totalRatio == 0) {
            throw new IllegalArgumentException("ratios must sum to a positive value");
        }

        long[] shares = new long[ratios.length];
        long[] remainders = new long[ratios.length];
        long allocated = 0;
        for (int i = 0; i < ratios.length; i++) {
            long product = Math.multiplyExact(minor, ratios[i]);
            shares[i] = Math.floorDiv(product, totalRatio);
            remainders[i] = Math.floorMod(product, totalRatio);
            allocated = Math.addExact(allocated, shares[i]);
        }

        long leftover = minor - allocated;
        Integer[] byRemainderDesc = new Integer[ratios.length];
        for (int i = 0; i < ratios.length; i++) {
            byRemainderDesc[i] = i;
        }
        java.util.Arrays.sort(
                byRemainderDesc,
                Comparator.<Integer>comparingLong(i -> remainders[i]).reversed());
        for (int i = 0; i < leftover; i++) {
            shares[byRemainderDesc[i]] += 1;
        }

        List<Money> parts = new ArrayList<>(ratios.length);
        for (long share : shares) {
            parts.add(new Money(share, currency));
        }
        return parts;
    }

    private Money sameCurrency(Money other) {
        if (!currency.equals(other.currency())) {
            throw new CurrencyMismatch(currency, other.currency());
        }
        return other;
    }
}
