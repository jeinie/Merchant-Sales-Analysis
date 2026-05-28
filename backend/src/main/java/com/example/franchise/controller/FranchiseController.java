package com.example.franchise.controller;

import com.example.franchise.config.JwtAuthenticationFilter;
import com.example.franchise.domain.Franchise;
import com.example.franchise.domain.User;
import com.example.franchise.service.MockDataStore;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class FranchiseController {

    private final MockDataStore mockDataStore;

    public FranchiseController(MockDataStore mockDataStore) {
        this.mockDataStore = mockDataStore;
    }

    @GetMapping("/users")
    public List<User> getUsers(HttpServletRequest request) {
        User user = currentUser(request);
        if (!"ADMIN".equals(user.getRole())) {
            return List.of(user);
        }

        return mockDataStore.getPublicUsers();
    }

    @GetMapping("/franchises")
    public List<Franchise> getFranchises(HttpServletRequest request) {
        User user = currentUser(request);
        return mockDataStore.getFranchises(user.getId(), user.getRole());
    }

    @GetMapping("/averages")
    public Map<String, Object> getAverages() {
        return mockDataStore.getAverages();
    }

    @GetMapping("/admin/users")
    public ResponseEntity<?> getAdminUsers(HttpServletRequest request) {
        if (!isAdmin(request)) {
            return forbidden();
        }

        return ResponseEntity.ok(mockDataStore.getSalesUsers());
    }

    @PostMapping("/admin/assign-manager")
    public ResponseEntity<?> assignManager(HttpServletRequest request, @RequestBody Map<String, String> payload) {
        if (!isAdmin(request)) {
            return forbidden();
        }

        String franchiseId = payload.get("franchiseId");
        String managerId = payload.get("managerId");

        if (franchiseId == null || franchiseId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "가맹점 ID가 필요합니다."));
        }

        try {
            mockDataStore.assignManager(franchiseId, managerId);
            return ResponseEntity.ok(Map.of("message", "담당 영업사원이 변경되었습니다."));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @PostMapping("/admin/toggle-ai")
    public ResponseEntity<?> toggleAi(HttpServletRequest request, @RequestBody Map<String, Object> payload) {
        if (!isAdmin(request)) {
            return forbidden();
        }

        String userId = (String) payload.get("userId");
        Boolean canUseAI = (Boolean) payload.get("canUseAI");

        if (userId == null || userId.isBlank() || canUseAI == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "필수 매개변수가 누락되었습니다."));
        }

        try {
            mockDataStore.toggleAi(userId, canUseAI);
            return ResponseEntity.ok(Map.of("message", "AI 컨설팅 권한이 변경되었습니다."));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    private User currentUser(HttpServletRequest request) {
        return (User) request.getAttribute(JwtAuthenticationFilter.CURRENT_USER_ATTRIBUTE);
    }

    private boolean isAdmin(HttpServletRequest request) {
        User user = currentUser(request);
        return user != null && "ADMIN".equals(user.getRole());
    }

    private ResponseEntity<?> forbidden() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", "관리자 권한이 필요합니다."));
    }
}
