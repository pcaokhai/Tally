package com.tally.core.archfixtures.injection;

import org.springframework.beans.factory.annotation.Autowired;

/** Deliberate violator of {@code noFieldInjection}. Never used in production code. */
public class FieldInjectedFixture {

    @Autowired
    public String collaborator;
}
