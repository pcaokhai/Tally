package com.tally.core.kernel.logging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class MaskTest {

    @Test
    void should_keep_only_the_first_local_character_when_masking_an_email() {
        assertThat(Mask.email("john.doe@example.com")).isEqualTo("j***@example.com");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "not-an-email", "@example.com", "a@"})
    void should_mask_entirely_when_the_email_is_absent_or_unparseable(String value) {
        assertThat(Mask.email(value)).isEqualTo("***");
    }

    @Test
    void should_keep_a_prefix_and_the_last_four_characters_when_masking_a_secret() {
        assertThat(Mask.secret("sk_live_0123456789abcdef")).isEqualTo("sk_***cdef");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "short", "1234567"})
    void should_mask_entirely_when_the_secret_is_absent_or_too_short(String value) {
        assertThat(Mask.secret(value)).isEqualTo("***");
    }
}
