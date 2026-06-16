package com.example.franchise.controller;

import com.example.franchise.config.JwtAuthenticationFilter;
import com.example.franchise.domain.AiInsightHistory;
import com.example.franchise.domain.Franchise;
import com.example.franchise.domain.FranchiseAlert;
import com.example.franchise.domain.User;
import com.example.franchise.service.FranchiseDataStore;
import com.example.franchise.service.FranchiseRiskService;
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

    private final FranchiseDataStore dataStore;
    private final FranchiseRiskService riskService;

    public FranchiseController(FranchiseDataStore dataStore, FranchiseRiskService riskService) {
        this.dataStore = dataStore;
        this.riskService = riskService;
    }

    @GetMapping("/users")
    public List<User> getUsers(HttpServletRequest request) {
        User user = currentUser(request);
        if (!"ADMIN".equals(user.getRole())) {
            return List.of(user);
        }

        return dataStore.getPublicUsers();
    }

    @GetMapping("/franchises")
    public List<Franchise> getFranchises(HttpServletRequest request) {
        User user = currentUser(request);
        return riskService.enrich(dataStore.getFranchises(user.getId(), user.getRole()));
    }

    @GetMapping("/alerts")
    public List<FranchiseAlert> getAlerts(HttpServletRequest request) {
        User user = currentUser(request);
        return riskService.alerts(dataStore.getFranchises(user.getId(), user.getRole()));
    }

    @GetMapping("/franchises/{franchiseId}/ai-insights")
    public ResponseEntity<?> getAiInsights(HttpServletRequest request, @org.springframework.web.bind.annotation.PathVariable String franchiseId) {
        if (!canAccessFranchise(request, franchiseId)) {
            return forbidden();
        }

        return ResponseEntity.ok(dataStore.getAiInsights(franchiseId));
    }

    @GetMapping("/franchises/{franchiseId}/ai-insights/latest")
    public ResponseEntity<?> getLatestAiInsight(HttpServletRequest request, @org.springframework.web.bind.annotation.PathVariable String franchiseId) {
        if (!canAccessFranchise(request, franchiseId)) {
            return forbidden();
        }

        AiInsightHistory latest = dataStore.getLatestAiInsight(franchiseId);
        if (latest == null) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(latest);
    }

    @PostMapping("/franchises/{franchiseId}/ai-insights")
    public ResponseEntity<?> saveAiInsight(
            HttpServletRequest request,
            @org.springframework.web.bind.annotation.PathVariable String franchiseId,
            @RequestBody Map<String, Object> payload) {
        User user = currentUser(request);
        if (!canAccessFranchise(request, franchiseId)) {
            return forbidden();
        }

        if (user.getPermissions() != null && Boolean.FALSE.equals(user.getPermissions().get("canUseAI"))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "AI 분석 권한이 필요합니다."));
        }

        String salesMonth = stringValue(payload.get("salesMonth"));
        String riskLevel = stringValue(payload.get("riskLevel"));
        String summary = stringValue(payload.get("summary"));
        String content = stringValue(payload.get("content"));
        List<String> tags = stringList(payload.get("tags"));

        if (salesMonth.isBlank() || riskLevel.isBlank() || summary.isBlank() || content.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "AI 인사이트 저장 필수값이 누락되었습니다."));
        }

        AiInsightHistory saved = dataStore.saveAiInsight(
                franchiseId,
                user.getId(),
                salesMonth,
                riskLevel,
                summary.length() > 500 ? summary.substring(0, 500) : summary,
                content,
                tags);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/averages")
    public Map<String, Object> getAverages() {
        return dataStore.getAverages();
    }

    @GetMapping("/admin/users")
    public ResponseEntity<?> getAdminUsers(HttpServletRequest request) {
        if (!isAdmin(request)) {
            return forbidden();
        }

        return ResponseEntity.ok(dataStore.getSalesUsers());
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
            dataStore.assignManager(franchiseId, managerId);
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
            dataStore.toggleAi(userId, canUseAI);
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

    private boolean canAccessFranchise(HttpServletRequest request, String franchiseId) {
        User user = currentUser(request);
        if (user == null || franchiseId == null || franchiseId.isBlank()) {
            return false;
        }

        return dataStore.getFranchises(user.getId(), user.getRole()).stream()
                .anyMatch(franchise -> franchiseId.equals(franchise.getId()));
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private List<String> stringList(Object value) {
        if (!(value instanceof List<?> values)) {
            return List.of();
        }

        return values.stream()
                .map(String::valueOf)
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .toList();
    }

    private ResponseEntity<?> forbidden() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", "관리자 권한이 필요합니다."));
    }
}
