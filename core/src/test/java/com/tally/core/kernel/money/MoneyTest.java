package com.tally.core.kernel.money;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Currency;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class MoneyTest {

    private static final Currency EUR = Currency.getInstance("EUR");
    private static final Currency USD = Currency.getInstance("USD");

    @Test
    void should_add_minor_units_when_currencies_match__TLY_004_AC1() {
        assertThat(new Money(1250, EUR).plus(new Money(99, EUR))).isEqualTo(new Money(1349, EUR));
    }

    @Test
    void should_subtract_minor_units_when_currencies_match__TLY_004_AC1() {
        assertThat(new Money(1250, EUR).minus(new Money(1300, EUR))).isEqualTo(new Money(-50, EUR));
    }

    @Test
    void should_throw_when_currencies_differ__TLY_004_AC1() {
        assertThatThrownBy(() -> new Money(1, EUR).plus(new Money(1, USD)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("EUR")
                .hasMessageContaining("USD");
    }

    @Test
    void should_throw_when_addition_overflows__TLY_004_AC1() {
        assertThatThrownBy(() -> new Money(Long.MAX_VALUE, EUR).plus(new Money(1, EUR)))
                .isInstanceOf(ArithmeticException.class);
    }

    @Test
    void should_multiply_minor_units_when_factor_given__TLY_201_AC1() {
        assertThat(new Money(150, EUR).times(3)).isEqualTo(new Money(450, EUR));
    }

    @Test
    void should_negate_minor_units__TLY_201_AC1() {
        assertThat(new Money(150, EUR).negate()).isEqualTo(new Money(-150, EUR));
    }

    @Test
    void should_report_zero_when_minor_units_are_zero__TLY_201_AC1() {
        assertThat(new Money(0, EUR).isZero()).isTrue();
        assertThat(new Money(1, EUR).isZero()).isFalse();
    }

    @Test
    void should_throw_when_multiplication_overflows__TLY_201_AC1() {
        assertThatThrownBy(() -> new Money(Long.MAX_VALUE, EUR).times(2)).isInstanceOf(ArithmeticException.class);
    }

    @Test
    void should_throw_when_negating_min_value__TLY_201_AC1() {
        assertThatThrownBy(() -> new Money(Long.MIN_VALUE, EUR).negate()).isInstanceOf(ArithmeticException.class);
    }

    @Test
    void should_throw_currency_mismatch_when_currencies_differ__TLY_201_AC2() {
        assertThatThrownBy(() -> new Money(1, EUR).plus(new Money(1, USD)))
                .isInstanceOf(CurrencyMismatch.class)
                .hasMessageContaining("EUR")
                .hasMessageContaining("USD");
    }

    @Test
    void should_allocate_without_losing_a_minor_unit__TLY_201_AC4() {
        List<Money> parts = new Money(100, EUR).allocate(1, 1, 1);
        assertThat(parts).hasSize(3);
        Money sum = parts.stream().reduce(new Money(0, EUR), Money::plus);
        assertThat(sum).isEqualTo(new Money(100, EUR));
    }

    @Test
    void should_reject_allocation_with_no_ratios__TLY_201_AC4() {
        assertThatThrownBy(() -> new Money(100, EUR).allocate()).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_allocate_10000_random_amounts_without_losing_a_minor_unit__TLY_201_AC4() {
        Random random = new Random(42);
        for (int i = 0; i < 10_000; i++) {
            long minor = random.nextLong(0, Integer.MAX_VALUE);
            int ratioCount = random.nextInt(1, 8);
            long[] ratios = new long[ratioCount];
            for (int r = 0; r < ratioCount; r++) {
                ratios[r] = random.nextLong(1, 100);
            }
            Money whole = new Money(minor, EUR);

            List<Money> parts = whole.allocate(ratios);

            Money sum = parts.stream().reduce(new Money(0, EUR), Money::plus);
            assertThat(sum)
                    .as("iteration %d, minor=%d, ratios=%s", i, minor, java.util.Arrays.toString(ratios))
                    .isEqualTo(whole);
        }
    }
}
