package com.example.merchant.service;

import com.example.merchant.domain.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    @Test
    void issuesAndVerifiesJwtClaims() {
        JwtService jwtService = new JwtService(new ObjectMapper(), "test-secret", 10);
        User user = user();

        JwtService.IssuedToken issuedToken = jwtService.issue(user);
        JwtService.Claims claims = jwtService.verify(issuedToken.token());

        assertThat(claims.userId()).isEqualTo("sales_user");
        assertThat(claims.role()).isEqualTo("SALES");
        assertThat(claims.name()).isEqualTo("Sales User");
        assertThat(claims.expiresAt()).isAfter(Instant.now());
    }

    @Test
    void rejectsTamperedJwt() {
        JwtService jwtService = new JwtService(new ObjectMapper(), "test-secret", 10);
        String token = jwtService.issue(user()).token();
        String tamperedToken = token.substring(0, token.length() - 1) + "x";

        assertThatThrownBy(() -> jwtService.verify(tamperedToken))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsExpiredJwt() {
        JwtService jwtService = new JwtService(new ObjectMapper(), "test-secret", -1);
        String token = jwtService.issue(user()).token();

        assertThatThrownBy(() -> jwtService.verify(token))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("만료");
    }

    private User user() {
        User user = new User();
        user.setId("sales_user");
        user.setName("Sales User");
        user.setRole("SALES");
        user.setAssignedMerchantIds(List.of("M001"));
        user.setPermissions(Map.of("canUseAI", true));
        return user;
    }
}
