package com.tally.core.kernel.money;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Currency;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.json.JsonMapper;

class MoneyModuleTest {

    private static final Currency EUR = Currency.getInstance("EUR");
    private final ObjectMapper mapper =
            JsonMapper.builder().addModule(new MoneyModule()).build();

    @Test
    void should_serialize_to_lowercase_amount_and_currency_fields__TLY_201_AC3() {
        String json = mapper.writeValueAsString(new Money(1250, EUR));

        assertThat(json).isEqualTo("{\"amount\":1250,\"currency\":\"EUR\"}");
    }

    @Test
    void should_deserialize_from_lowercase_amount_and_currency_fields__TLY_201_AC3() {
        Money money = mapper.readValue("{\"amount\":1250,\"currency\":\"EUR\"}", Money.class);

        assertThat(money).isEqualTo(new Money(1250, EUR));
    }

    @Test
    void should_reject_decimal_amount_on_input__TLY_201_AC3() {
        assertThatThrownBy(() -> mapper.readValue("{\"amount\":12.50,\"currency\":\"EUR\"}", Money.class))
                .isInstanceOf(InvalidFormatException.class);
    }
}
