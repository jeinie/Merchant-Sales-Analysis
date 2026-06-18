package com.example.merchant.controller;

import com.example.merchant.config.JwtAuthenticationFilter;
import com.example.merchant.domain.AiInsightHistory;
import com.example.merchant.domain.Merchant;
import com.example.merchant.domain.MerchantAlert;
import com.example.merchant.domain.User;
import com.example.merchant.service.AiInsightGenerationService;
import com.example.merchant.service.MerchantDataStore;
import com.example.merchant.service.MerchantRiskService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class MerchantController {
    private static final Set<String> LOCATION_STATUSES = Set.of(
            "UNVERIFIED",
            "GEOCODED",
            "VERIFIED",
            "FAILED",
            "MANUAL");
    private static final Set<String> INACTIVE_OPERATIONAL_STATUSES = Set.of(
            "CLOSED",
            "CONTRACT_ENDED",
            "SUSPENDED");


    private final MerchantDataStore dataStore;
    private final MerchantRiskService riskService;
    private final AiInsightGenerationService aiInsightGenerationService;

    public MerchantController(
            MerchantDataStore dataStore,
            MerchantRiskService riskService,
            AiInsightGenerationService aiInsightGenerationService) {
        this.dataStore = dataStore;
        this.riskService = riskService;
        this.aiInsightGenerationService = aiInsightGenerationService;
    }

    @GetMapping("/users")
    public List<User> getUsers(HttpServletRequest request) {
        User user = currentUser(request);
        if (!"ADMIN".equals(user.getRole())) {
            return List.of(user);
        }

        return dataStore.getPublicUsers();
    }

    @GetMapping("/merchants")
    public List<Merchant> getMerchants(HttpServletRequest request) {
        User user = currentUser(request);
        return riskService.enrich(dataStore.getMerchants(user.getId(), user.getRole()));
    }

    @GetMapping("/alerts")
    public List<MerchantAlert> getAlerts(HttpServletRequest request) {
        User user = currentUser(request);
        return riskService.alerts(dataStore.getMerchants(user.getId(), user.getRole()));
    }

    @GetMapping("/merchants/{merchantId}/ai-insights")
    public ResponseEntity<?> getAiInsights(HttpServletRequest request, @org.springframework.web.bind.annotation.PathVariable String merchantId) {
        if (!canAccessMerchant(request, merchantId)) {
            return forbidden();
        }

        return ResponseEntity.ok(dataStore.getAiInsights(merchantId));
    }

    @GetMapping("/merchants/{merchantId}/ai-insights/latest")
    public ResponseEntity<?> getLatestAiInsight(HttpServletRequest request, @org.springframework.web.bind.annotation.PathVariable String merchantId) {
        if (!canAccessMerchant(request, merchantId)) {
            return forbidden();
        }

        AiInsightHistory latest = dataStore.getLatestAiInsight(merchantId);
        if (latest == null) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(latest);
    }

    @PostMapping("/merchants/{merchantId}/ai-insights")
    public ResponseEntity<?> saveAiInsight(
            HttpServletRequest request,
            @org.springframework.web.bind.annotation.PathVariable String merchantId,
            @RequestBody Map<String, Object> payload) {
        User user = currentUser(request);
        if (!canAccessMerchant(request, merchantId)) {
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
                merchantId,
                user.getId(),
                salesMonth,
                riskLevel,
                summary.length() > 500 ? summary.substring(0, 500) : summary,
                content,
                stringValue(payload.get("note")),
                tags);
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/merchants/{merchantId}/ai-insights/generate")
    public ResponseEntity<?> generateAiInsight(
            HttpServletRequest request,
            @org.springframework.web.bind.annotation.PathVariable String merchantId) {
        User user = currentUser(request);
        Merchant merchant = findAccessibleMerchant(request, merchantId);
        if (merchant == null) {
            return forbidden();
        }

        if (user.getPermissions() != null && Boolean.FALSE.equals(user.getPermissions().get("canUseAI"))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "AI 분석 권한이 필요합니다."));
        }

        if (merchant.getMonthlySales() == null || merchant.getMonthlySales().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "AI 분석에 필요한 매출 데이터가 없습니다."));
        }

        try {
            String content = aiInsightGenerationService.generate(merchant, dataStore.getAverages());
            String salesMonth = merchant.getMonthlySales().get(merchant.getMonthlySales().size() - 1).getMonth();
            AiInsightHistory saved = dataStore.saveAiInsight(
                    merchantId,
                    user.getId(),
                    salesMonth,
                    fallback(merchant.getRiskLevel(), "NORMAL"),
                    truncate(fallback(merchant.getRiskSummary(), merchant.getName() + " AI 운영 인사이트"), 500),
                    content,
                    "",
                    merchant.getAlertTags() == null ? List.of() : merchant.getAlertTags());
            return ResponseEntity.ok(saved);
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("message", ex.getMessage()));
        }
    }

    @PostMapping("/merchants/{merchantId}/ai-insights/{insightId}/note")
    public ResponseEntity<?> updateAiInsightNote(
            HttpServletRequest request,
            @org.springframework.web.bind.annotation.PathVariable String merchantId,
            @org.springframework.web.bind.annotation.PathVariable Long insightId,
            @RequestBody Map<String, Object> payload) {
        if (!canAccessMerchant(request, merchantId)) {
            return forbidden();
        }

        try {
            AiInsightHistory updated = dataStore.updateAiInsightNote(
                    insightId,
                    merchantId,
                    stringValue(payload.get("note")));
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
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

        User user = currentUser(request);
        String merchantId = payload.get("merchantId");
        String managerId = payload.get("managerId");

        if (merchantId == null || merchantId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "가맹점 ID가 필요합니다."));
        }

        try {
            dataStore.assignManager(merchantId, managerId, user.getId(), fallback(payload.get("changeReason"), "관리자 화면에서 담당자를 변경했습니다."));
            return ResponseEntity.ok(Map.of("message", "담당 영업사원이 변경되었습니다."));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @GetMapping("/admin/assignment-histories")
    public ResponseEntity<?> getAssignmentHistories(HttpServletRequest request) {
        if (!isAdmin(request)) {
            return forbidden();
        }

        return ResponseEntity.ok(dataStore.getAssignmentHistories());
    }

    @PostMapping("/admin/merchants")
    public ResponseEntity<?> createMerchant(HttpServletRequest request, @RequestBody Map<String, Object> payload) {
        if (!isAdmin(request)) {
            return forbidden();
        }

        String name = stringValue(payload.get("name"));
        String industry = stringValue(payload.get("industry"));
        String region = stringValue(payload.get("region"));
        String address = stringValue(payload.get("address"));
        String managerId = stringValue(payload.get("managerId"));
        String locationStatus = fallback(stringValue(payload.get("locationStatus")), "UNVERIFIED").toUpperCase();

        if (name.isBlank() || industry.isBlank() || region.isBlank() || address.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "가맹점명, 업종, 지역, 주소는 필수입니다."));
        }
        if (!LOCATION_STATUSES.contains(locationStatus)) {
            return ResponseEntity.badRequest().body(Map.of("message", "지원하지 않는 위치 상태입니다."));
        }

        Double latitude;
        Double longitude;
        try {
            latitude = nullableDouble(payload.get("latitude"));
            longitude = nullableDouble(payload.get("longitude"));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }

        ResponseEntity<?> coordinateError = validateCoordinates(latitude, longitude, locationStatus);
        if (coordinateError != null) {
            return coordinateError;
        }

        try {
            Merchant saved = dataStore.createMerchant(
                    truncate(name, 120),
                    truncate(industry, 60),
                    truncate(region, 100),
                    truncate(address, 255),
                    latitude,
                    longitude,
                    locationStatus,
                    truncate(stringValue(payload.get("geocodeSource")), 50),
                    truncate(stringValue(payload.get("locationNote")), 255),
                    managerId,
                    currentUser(request).getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @PutMapping("/admin/merchants/{merchantId}")
    public ResponseEntity<?> updateMerchant(
            HttpServletRequest request,
            @PathVariable String merchantId,
            @RequestBody Map<String, Object> payload) {
        if (!isAdmin(request)) {
            return forbidden();
        }

        String name = stringValue(payload.get("name"));
        String industry = stringValue(payload.get("industry"));
        String region = stringValue(payload.get("region"));
        String address = stringValue(payload.get("address"));
        String locationStatus = fallback(stringValue(payload.get("locationStatus")), "UNVERIFIED").toUpperCase();

        if (name.isBlank() || industry.isBlank() || region.isBlank() || address.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "가맹점명, 업종, 지역, 주소는 필수입니다."));
        }
        if (!LOCATION_STATUSES.contains(locationStatus)) {
            return ResponseEntity.badRequest().body(Map.of("message", "지원하지 않는 위치 상태입니다."));
        }

        Double latitude;
        Double longitude;
        try {
            latitude = nullableDouble(payload.get("latitude"));
            longitude = nullableDouble(payload.get("longitude"));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }

        ResponseEntity<?> coordinateError = validateCoordinates(latitude, longitude, locationStatus);
        if (coordinateError != null) {
            return coordinateError;
        }

        try {
            Merchant updated = dataStore.updateMerchant(
                    merchantId,
                    truncate(name, 120),
                    truncate(industry, 60),
                    truncate(region, 100),
                    truncate(address, 255),
                    latitude,
                    longitude,
                    locationStatus,
                    truncate(stringValue(payload.get("geocodeSource")), 50),
                    truncate(stringValue(payload.get("locationNote")), 255));
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @PostMapping("/admin/merchants/{merchantId}/close")
    public ResponseEntity<?> closeMerchant(
            HttpServletRequest request,
            @PathVariable String merchantId,
            @RequestBody(required = false) Map<String, Object> payload) {
        if (!isAdmin(request)) {
            return forbidden();
        }

        try {
            String closureNote = payload == null ? "" : stringValue(payload.get("closureNote"));
            dataStore.updateMerchantStatus(merchantId, "CLOSED", truncate(closureNote, 255));
            return ResponseEntity.ok(Map.of("message", "가맹점이 폐점 처리되었습니다."));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @PostMapping("/admin/merchants/{merchantId}/status")
    public ResponseEntity<?> updateMerchantStatus(
            HttpServletRequest request,
            @PathVariable String merchantId,
            @RequestBody(required = false) Map<String, Object> payload) {
        if (!isAdmin(request)) {
            return forbidden();
        }

        String operationalStatus = payload == null ? "" : stringValue(payload.get("operationalStatus")).toUpperCase();
        if (!INACTIVE_OPERATIONAL_STATUSES.contains(operationalStatus)) {
            return ResponseEntity.badRequest().body(Map.of("message", "지원하지 않는 가맹점 관리 상태입니다."));
        }

        try {
            String statusNote = payload == null ? "" : stringValue(payload.get("statusNote"));
            dataStore.updateMerchantStatus(merchantId, operationalStatus, truncate(statusNote, 255));
            return ResponseEntity.ok(Map.of("message", "가맹점 관리 상태가 변경되었습니다."));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @PostMapping("/admin/merchants/{merchantId}/location")
    public ResponseEntity<?> updateMerchantLocation(
            HttpServletRequest request,
            @PathVariable String merchantId,
            @RequestBody Map<String, Object> payload) {
        if (!isAdmin(request)) {
            return forbidden();
        }

        String locationStatus = fallback(stringValue(payload.get("locationStatus")), "UNVERIFIED").toUpperCase();
        if (!LOCATION_STATUSES.contains(locationStatus)) {
            return ResponseEntity.badRequest().body(Map.of("message", "지원하지 않는 위치 상태입니다."));
        }

        Double latitude;
        Double longitude;
        try {
            latitude = nullableDouble(payload.get("latitude"));
            longitude = nullableDouble(payload.get("longitude"));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
        ResponseEntity<?> coordinateError = validateCoordinates(latitude, longitude, locationStatus);
        if (coordinateError != null) {
            return coordinateError;
        }

        try {
            Merchant updated = dataStore.updateMerchantLocation(
                    merchantId,
                    latitude,
                    longitude,
                    locationStatus,
                    truncate(stringValue(payload.get("geocodeSource")), 50),
                    truncate(stringValue(payload.get("locationNote")), 255));
            return ResponseEntity.ok(updated);
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

    private boolean canAccessMerchant(HttpServletRequest request, String merchantId) {
        return findAccessibleMerchant(request, merchantId) != null;
    }

    private Merchant findAccessibleMerchant(HttpServletRequest request, String merchantId) {
        User user = currentUser(request);
        if (user == null || merchantId == null || merchantId.isBlank()) {
            return null;
        }

        return riskService.enrich(dataStore.getMerchants(user.getId(), user.getRole())).stream()
                .filter(merchant -> merchantId.equals(merchant.getId()))
                .findFirst()
                .orElse(null);
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

    private Double nullableDouble(Object value) {
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }

        if (value instanceof Number number) {
            return number.doubleValue();
        }

        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("숫자 형식이 올바르지 않습니다.");
        }
    }

    private ResponseEntity<?> validateCoordinates(Double latitude, Double longitude, String locationStatus) {
        if ((latitude == null) != (longitude == null)) {
            return ResponseEntity.badRequest().body(Map.of("message", "위도와 경도는 함께 입력해야 합니다."));
        }
        if (latitude != null && (latitude < -90 || latitude > 90)) {
            return ResponseEntity.badRequest().body(Map.of("message", "위도는 -90에서 90 사이여야 합니다."));
        }
        if (longitude != null && (longitude < -180 || longitude > 180)) {
            return ResponseEntity.badRequest().body(Map.of("message", "경도는 -180에서 180 사이여야 합니다."));
        }
        if (("GEOCODED".equals(locationStatus) || "VERIFIED".equals(locationStatus) || "MANUAL".equals(locationStatus))
                && (latitude == null || longitude == null)) {
            return ResponseEntity.badRequest().body(Map.of("message", "좌표 확인 상태에는 위도와 경도가 필요합니다."));
        }
        return null;
    }

    private String fallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return "";
        }

        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }

    private ResponseEntity<?> forbidden() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", "관리자 권한이 필요합니다."));
    }
}
