package com.example.franchise.config;

import com.example.franchise.domain.User;
import com.example.franchise.service.JwtService;
import com.example.franchise.service.MockDataStore;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String CURRENT_USER_ATTRIBUTE = "currentUser";

    private final JwtService jwtService;
    private final MockDataStore mockDataStore;

    public JwtAuthenticationFilter(JwtService jwtService, MockDataStore mockDataStore) {
        this.jwtService = jwtService;
        this.mockDataStore = mockDataStore;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        if (shouldSkip(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            writeUnauthorized(response, "인증 토큰이 필요합니다.");
            return;
        }

        try {
            JwtService.Claims claims = jwtService.verify(authorization.substring("Bearer ".length()).trim());
            User user = mockDataStore.findPublicUserById(claims.userId());
            if (user == null) {
                writeUnauthorized(response, "유효하지 않은 사용자입니다.");
                return;
            }

            request.setAttribute(CURRENT_USER_ATTRIBUTE, user);
            filterChain.doFilter(request, response);
        } catch (IllegalArgumentException ex) {
            writeUnauthorized(response, ex.getMessage());
        }
    }

    private boolean shouldSkip(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (!path.startsWith("/api")) {
            return true;
        }

        return "OPTIONS".equalsIgnoreCase(request.getMethod())
                || "/api/auth/login".equals(path)
                || "/api/auth/test-users".equals(path);
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        response.getWriter().write("{\"message\":\"" + escapeJson(message) + "\"}");
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
