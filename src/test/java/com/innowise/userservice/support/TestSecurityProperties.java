package com.innowise.userservice.support;

import org.springframework.test.context.DynamicPropertyRegistry;

public final class TestSecurityProperties {

    public static final String REGISTRATION_KEY =
            "test-registration-key-0123456789abcdef";

    private TestSecurityProperties() {}

    public static void register(DynamicPropertyRegistry registry) {
        registry.add("user.registration-key", () -> REGISTRATION_KEY);
    }
}
