package com.example.merchant.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordHasherTest {

    private final PasswordHasher passwordHasher = new PasswordHasher();

    @Test
    void hashesPasswordWithSaltAndVerifiesOriginalPassword() {
        String firstHash = passwordHasher.hash("1234");
        String secondHash = passwordHasher.hash("1234");

        assertThat(firstHash).startsWith("pbkdf2_sha256$");
        assertThat(firstHash).isNotEqualTo(secondHash);
        assertThat(passwordHasher.matches("1234", firstHash)).isTrue();
        assertThat(passwordHasher.matches("wrong-password", firstHash)).isFalse();
    }
}
