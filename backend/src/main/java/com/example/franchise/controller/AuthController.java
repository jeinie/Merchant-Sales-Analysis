package com.example.franchise.controller;

import com.example.franchise.domain.User;
import com.example.franchise.service.FranchiseDataStore;
import com.example.franchise.service.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final JwtService jwtService;
    private final FranchiseDataStore dataStore;

    public AuthController(JwtService jwtService, FranchiseDataStore dataStore) {
        this.jwtService = jwtService;
        this.dataStore = dataStore;
    }

    @GetMapping("/test-users")
    public ResponseEntity<?> getTestUsers() {
        return ResponseEntity.ok(dataStore.getPublicUsers());
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        String id = credentials.get("id");
        String password = credentials.get("password");

        if (id == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "아이디와 비밀번호를 입력해주세요."));
        }

        User user = dataStore.login(id, password);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "아이디 또는 비밀번호가 올바르지 않습니다."));
        }

        JwtService.IssuedToken token = jwtService.issue(user);
        return ResponseEntity.ok(Map.of(
                "token", token.token(),
                "expiresAt", token.expiresAt().toString(),
                "user", user));
    }
}
