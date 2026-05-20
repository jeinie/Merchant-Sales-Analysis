package com.example.franchise.controller;

import com.example.franchise.domain.Franchise;
import com.example.franchise.domain.User;
import com.example.franchise.service.MockDataStore;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class FranchiseController {

    private final MockDataStore mockDataStore;

    @GetMapping("/users")
    public List<User> getUsers() {
        return mockDataStore.getPublicUsers();
    }

    @GetMapping("/franchises")
    public List<Franchise> getFranchises(
            @RequestParam(value = "userId", required = false) String userId,
            @RequestParam(value = "role", required = false) String role) {
        return mockDataStore.getFranchises(userId, role);
    }

    @GetMapping("/averages")
    public Map<String, Object> getAverages() {
        return mockDataStore.getAverages();
    }

    @GetMapping("/admin/users")
    public List<User> getAdminUsers() {
        return mockDataStore.getSalesUsers();
    }

    @PostMapping("/admin/assign-manager")
    public ResponseEntity<?> assignManager(@RequestBody Map<String, String> payload) {
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
    public ResponseEntity<?> toggleAi(@RequestBody Map<String, Object> payload) {
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
}
