package com.example.franchise.service;

import com.example.franchise.domain.User;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class JwtService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final ObjectMapper objectMapper;
    private final String jwtSecret;
    private final long expirationMinutes;

    public JwtService(
            ObjectMapper objectMapper,
            @Value("${app.auth.jwt-secret}") String jwtSecret,
            @Value("${app.auth.jwt-expiration-minutes}") long expirationMinutes) {
        this.objectMapper = objectMapper;
        this.jwtSecret = jwtSecret;
        this.expirationMinutes = expirationMinutes;
    }

    public IssuedToken issue(User user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(expirationMinutes, ChronoUnit.MINUTES);

        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", "HS256");
        header.put("typ", "JWT");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sub", user.getId());
        payload.put("role", user.getRole());
        payload.put("name", user.getName());
        payload.put("iat", now.getEpochSecond());
        payload.put("exp", expiresAt.getEpochSecond());

        String headerPart = encodeJson(header);
        String payloadPart = encodeJson(payload);
        String signaturePart = sign(headerPart + "." + payloadPart);

        return new IssuedToken(headerPart + "." + payloadPart + "." + signaturePart, expiresAt);
    }

    public Claims verify(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("토큰이 없습니다.");
        }

        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("토큰 형식이 올바르지 않습니다.");
        }

        String expectedSignature = sign(parts[0] + "." + parts[1]);
        if (!MessageDigest.isEqual(
                expectedSignature.getBytes(StandardCharsets.UTF_8),
                parts[2].getBytes(StandardCharsets.UTF_8))) {
            throw new IllegalArgumentException("토큰 서명이 올바르지 않습니다.");
        }

        try {
            Map<String, Object> header = decodeJson(parts[0]);
            if (!"HS256".equals(header.get("alg"))) {
                throw new IllegalArgumentException("지원하지 않는 토큰 알고리즘입니다.");
            }

            Map<String, Object> payload = decodeJson(parts[1]);
            String userId = (String) payload.get("sub");
            String role = (String) payload.get("role");
            String name = (String) payload.get("name");
            long exp = ((Number) payload.get("exp")).longValue();
            Instant expiresAt = Instant.ofEpochSecond(exp);

            if (Instant.now().isAfter(expiresAt)) {
                throw new IllegalArgumentException("토큰이 만료되었습니다.");
            }

            return new Claims(userId, role, name, expiresAt);
        } catch (ClassCastException | NullPointerException ex) {
            throw new IllegalArgumentException("토큰 내용이 올바르지 않습니다.", ex);
        }
    }

    private String encodeJson(Map<String, Object> value) {
        try {
            byte[] json = objectMapper.writeValueAsBytes(value);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(json);
        } catch (Exception ex) {
            throw new IllegalStateException("토큰 생성에 실패했습니다.", ex);
        }
    }

    private Map<String, Object> decodeJson(String value) {
        try {
            byte[] json = Base64.getUrlDecoder().decode(value);
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception ex) {
            throw new IllegalArgumentException("토큰 JSON 파싱에 실패했습니다.", ex);
        }
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
            mac.init(keySpec);
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("토큰 서명에 실패했습니다.", ex);
        }
    }

    public record IssuedToken(String token, Instant expiresAt) {
    }

    public record Claims(String userId, String role, String name, Instant expiresAt) {
    }
}
