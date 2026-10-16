package com.tally.core.kernel.money;

import java.util.Currency;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.module.SimpleModule;

/**
 * Jackson 3 module for {@link Money} (ADR-004, TLY-201-AC3): wire format is
 * {@code {"amount": <integer minor units>, "currency": "<ISO code>"}}. A non-integral amount is
 * rejected rather than truncated or rounded.
 */
public class MoneyModule extends SimpleModule {

    public MoneyModule() {
        super("MoneyModule");
        addSerializer(Money.class, new Serializer());
        addDeserializer(Money.class, new Deserializer());
    }

    private static final class Serializer extends ValueSerializer<Money> {
        @Override
        public void serialize(Money value, JsonGenerator gen, SerializationContext ctxt) {
            gen.writeStartObject();
            gen.writeNumberProperty("amount", value.minor());
            gen.writeStringProperty("currency", value.currency().getCurrencyCode());
            gen.writeEndObject();
        }
    }

    private static final class Deserializer extends ValueDeserializer<Money> {
        @Override
        public Money deserialize(JsonParser p, DeserializationContext ctxt) {
            JsonNode node = ctxt.readTree(p);
            JsonNode amount = node.get("amount");
            if (amount == null || !amount.isIntegralNumber()) {
                throw InvalidFormatException.from(
                        p,
                        "Money amount must be an integer number of minor units",
                        amount == null ? null : amount.asString(),
                        Money.class);
            }
            Currency currency = Currency.getInstance(node.get("currency").asString());
            return new Money(amount.asLong(), currency);
        }
    }
}
