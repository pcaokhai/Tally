package com.tally.core.kernel.money;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Currency;
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
}
