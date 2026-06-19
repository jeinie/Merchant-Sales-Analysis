package com.example.merchant.controller;

import com.example.merchant.config.JwtAuthenticationFilter;
import com.example.merchant.domain.AiInsightHistory;
import com.example.merchant.domain.Merchant;
import com.example.merchant.domain.MerchantAlert;
import com.example.merchant.domain.MonthlySales;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
    private static final Set<String> OPERATIONAL_STATUSES = Set.of(
            "ACTIVE",
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

    @GetMapping("/admin/merchants")
    public ResponseEntity<?> getAdminMerchants(
            HttpServletRequest request,
            @RequestParam(defaultValue = "ACTIVE") String status) {
        if (!isAdmin(request)) {
            return forbidden();
        }

        try {
            return ResponseEntity.ok(riskService.enrich(dataStore.getAdminMerchants(status)));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @GetMapping("/alerts")
    public List<MerchantAlert> getAlerts(HttpServletRequest request) {
        User user = currentUser(request);
        return riskService.alerts(dataStore.getMerchants(user.getId(), user.getRole()));
    }

    @PostMapping("/admin/sales-upload/preview")
    public ResponseEntity<?> previewSalesUpload(
            HttpServletRequest request,
            @RequestParam("file") MultipartFile file) {
        if (!isAdmin(request)) {
            return forbidden();
        }
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "업로드할 CSV 파일을 선택해주세요."));
        }

        try {
            return ResponseEntity.ok(buildSalesUploadPreview(file));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "CSV 파일을 읽는 중 오류가 발생했습니다."));
        }
    }

    @PostMapping("/admin/sales-upload/commit")
    public ResponseEntity<?> commitSalesUpload(
            HttpServletRequest request,
            @RequestParam("file") MultipartFile file) {
        if (!isAdmin(request)) {
            return forbidden();
        }
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "업로드할 CSV 파일을 선택해주세요."));
        }

        try {
            Map<String, Object> preview = buildSalesUploadPreview(file);
            List<Map<String, Object>> rows = (List<Map<String, Object>>) preview.get("rows");
            List<Map<String, Object>> errorRows = rows.stream()
                    .filter(row -> "ERROR".equals(row.get("status")))
                    .toList();
            if (!errorRows.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "message", "오류가 있는 행이 있어 매출 데이터를 반영할 수 없습니다.",
                        "summary", preview.get("summary"),
                        "rows", rows));
            }

            List<MonthlySales> salesRows = rows.stream()
                    .map(this::monthlySalesFromPreviewRow)
                    .toList();
            int affectedRows = dataStore.upsertMonthlySales(salesRows);
            return ResponseEntity.ok(Map.of(
                    "message", "매출 데이터가 반영되었습니다.",
                    "affectedRows", affectedRows,
                    "summary", preview.get("summary")));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "CSV 파일을 반영하는 중 오류가 발생했습니다."));
        }
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
        if (!OPERATIONAL_STATUSES.contains(operationalStatus)) {
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

    private Map<String, Object> buildSalesUploadPreview(MultipartFile file) throws Exception {
        List<Merchant> merchants = dataStore.getAdminMerchants("ACTIVE");
        Map<String, Merchant> merchantById = merchants.stream()
                .collect(Collectors.toMap(Merchant::getId, merchant -> merchant, (left, right) -> left));

        List<Map<String, Object>> rows = new ArrayList<>();
        Set<String> seenKeys = new java.util.HashSet<>();
        int errorRows = 0;
        int warningRows = 0;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null || headerLine.isBlank()) {
                throw new IllegalArgumentException("CSV 파일에 헤더가 없습니다.");
            }

            Map<String, Integer> headerIndex = csvHeaderIndex(headerLine);
            List<String> requiredHeaders = List.of("merchantId", "salesMonth", "sales", "txCount", "avgTicket");
            List<String> missingHeaders = requiredHeaders.stream()
                    .filter(header -> !headerIndex.containsKey(header))
                    .toList();
            if (!missingHeaders.isEmpty()) {
                throw new IllegalArgumentException("필수 컬럼이 누락되었습니다: " + String.join(", ", missingHeaders));
            }

            String line;
            int rowNumber = 1;
            while ((line = reader.readLine()) != null) {
                rowNumber++;
                if (line.isBlank()) {
                    continue;
                }

                Map<String, Object> row = validateSalesUploadRow(
                        rowNumber,
                        parseCsvLine(line),
                        headerIndex,
                        merchantById,
                        seenKeys);
                rows.add(row);
                if (!((List<?>) row.get("errors")).isEmpty()) {
                    errorRows++;
                } else if (!((List<?>) row.get("warnings")).isEmpty()) {
                    warningRows++;
                }
            }
        }

        int totalRows = rows.size();
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalRows", totalRows);
        summary.put("validRows", totalRows - errorRows);
        summary.put("errorRows", errorRows);
        summary.put("warningRows", warningRows);

        return Map.of("summary", summary, "rows", rows);
    }

    private Map<String, Object> validateSalesUploadRow(
            int rowNumber,
            List<String> columns,
            Map<String, Integer> headerIndex,
            Map<String, Merchant> merchantById,
            Set<String> seenKeys) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        String merchantId = csvValue(columns, headerIndex, "merchantId");
        String salesMonth = csvValue(columns, headerIndex, "salesMonth");
        Long sales = parseLong(csvValue(columns, headerIndex, "sales"), "매출", errors);
        Integer txCount = parseInteger(csvValue(columns, headerIndex, "txCount"), "거래 건수", errors);
        Integer avgTicket = parseInteger(csvValue(columns, headerIndex, "avgTicket"), "객단가", errors);
        Merchant merchant = merchantById.get(merchantId);

        if (merchantId.isBlank()) {
            errors.add("가맹점 ID가 비어 있습니다.");
        } else if (merchant == null) {
            errors.add("존재하지 않거나 관리 중이 아닌 가맹점 ID입니다.");
        }
        if (!salesMonth.matches("\\d{4}-\\d{2}")) {
            errors.add("매출 월은 YYYY-MM 형식이어야 합니다.");
        }

        String uniqueKey = merchantId + "|" + salesMonth;
        if (!merchantId.isBlank() && !salesMonth.isBlank() && !seenKeys.add(uniqueKey)) {
            errors.add("CSV 파일 안에 동일 가맹점/동일 월 데이터가 중복되었습니다.");
        }

        if (sales != null && sales < 0) {
            errors.add("매출은 음수일 수 없습니다.");
        }
        if (txCount != null && txCount < 0) {
            errors.add("거래 건수는 음수일 수 없습니다.");
        }
        if (avgTicket != null && avgTicket < 0) {
            errors.add("객단가는 음수일 수 없습니다.");
        }

        if (sales != null && txCount != null && avgTicket != null) {
            if (txCount == 0 && sales > 0) {
                warnings.add("거래 건수가 0인데 매출이 0보다 큽니다.");
            }
            if (txCount > 0) {
                long calculatedAvgTicket = Math.round((double) sales / txCount);
                long gap = Math.abs(calculatedAvgTicket - avgTicket);
                if (gap > Math.max(1000, Math.round(calculatedAvgTicket * 0.1))) {
                    warnings.add("업로드 객단가와 매출/거래건수 기준 계산값 차이가 큽니다.");
                }
            }
        }

        if (merchant != null && sales != null) {
            boolean duplicatedMonth = merchant.getMonthlySales().stream()
                    .anyMatch(monthlySales -> salesMonth.equals(monthlySales.getMonth()));
            if (duplicatedMonth) {
                warnings.add("이미 등록된 동일 월 매출 데이터가 있습니다.");
            }

            String previousMonth = previousMonth(salesMonth);
            merchant.getMonthlySales().stream()
                    .filter(monthlySales -> previousMonth.equals(monthlySales.getMonth()))
                    .findFirst()
                    .ifPresent(previous -> {
                        Long previousSales = previous.getSales();
                        if (previousSales != null && previousSales > 0) {
                            double changeRate = ((double) sales - previousSales) / previousSales * 100;
                            if (Math.abs(changeRate) >= 50) {
                                warnings.add("전월 대비 매출 변동이 50% 이상입니다.");
                            }
                        }
                    });
        }

        Map<String, Object> row = new LinkedHashMap<>();
        row.put("rowNumber", rowNumber);
        row.put("merchantId", merchantId);
        row.put("merchantName", merchant == null ? "" : merchant.getName());
        row.put("salesMonth", salesMonth);
        row.put("sales", sales);
        row.put("txCount", txCount);
        row.put("avgTicket", avgTicket);
        row.put("status", !errors.isEmpty() ? "ERROR" : (!warnings.isEmpty() ? "WARNING" : "VALID"));
        row.put("errors", errors);
        row.put("warnings", warnings);
        return row;
    }

    private MonthlySales monthlySalesFromPreviewRow(Map<String, Object> row) {
        MonthlySales monthlySales = new MonthlySales();
        monthlySales.setMerchantId(String.valueOf(row.get("merchantId")));
        monthlySales.setMonth(String.valueOf(row.get("salesMonth")));
        monthlySales.setSales((Long) row.get("sales"));
        monthlySales.setTxCount((Integer) row.get("txCount"));
        monthlySales.setAvgTicket((Integer) row.get("avgTicket"));
        return monthlySales;
    }

    private Map<String, Integer> csvHeaderIndex(String headerLine) {
        List<String> headers = parseCsvLine(headerLine);
        Map<String, Integer> result = new HashMap<>();
        for (int index = 0; index < headers.size(); index++) {
            result.put(headers.get(index).replace("\uFEFF", "").trim(), index);
        }
        return result;
    }

    private List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int index = 0; index < line.length(); index++) {
            char ch = line.charAt(index);
            if (ch == '"') {
                if (inQuotes && index + 1 < line.length() && line.charAt(index + 1) == '"') {
                    current.append('"');
                    index++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (ch == ',' && !inQuotes) {
                values.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        values.add(current.toString().trim());
        return values;
    }

    private String csvValue(List<String> columns, Map<String, Integer> headerIndex, String header) {
        Integer index = headerIndex.get(header);
        if (index == null || index >= columns.size()) {
            return "";
        }
        return columns.get(index).trim();
    }

    private Long parseLong(String value, String label, List<String> errors) {
        if (value.isBlank()) {
            errors.add(label + "이 비어 있습니다.");
            return null;
        }
        try {
            return Long.parseLong(value.replace(",", ""));
        } catch (NumberFormatException ex) {
            errors.add(label + "은 숫자여야 합니다.");
            return null;
        }
    }

    private Integer parseInteger(String value, String label, List<String> errors) {
        Long parsed = parseLong(value, label, errors);
        if (parsed == null) {
            return null;
        }
        if (parsed > Integer.MAX_VALUE) {
            errors.add(label + "이 허용 범위를 초과했습니다.");
            return null;
        }
        return parsed.intValue();
    }

    private String previousMonth(String salesMonth) {
        if (!salesMonth.matches("\\d{4}-\\d{2}")) {
            return "";
        }

        int year = Integer.parseInt(salesMonth.substring(0, 4));
        int month = Integer.parseInt(salesMonth.substring(5, 7));
        if (month == 1) {
            return "%04d-12".formatted(year - 1);
        }
        return "%04d-%02d".formatted(year, month - 1);
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
