package com.tally.core;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

class CoreApplicationTest {
    @Test
    void main_doesNotThrow__TLY_001_AC1() {
        assertDoesNotThrow(() -> CoreApplication.main(new String[0]));
    }
}
