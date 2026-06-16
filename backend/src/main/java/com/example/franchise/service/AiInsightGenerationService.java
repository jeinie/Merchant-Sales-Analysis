package com.example.franchise.service;

import com.example.franchise.domain.Franchise;
import com.example.franchise.domain.MonthlySales;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class AiInsightGenerationService {
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String apiKey;
    private final String model;

    public AiInsightGenerationService(
            ObjectMapper objectMapper,
            @Value("${app.ai.gemini-api-key:}") String apiKey,
            @Value("${app.ai.gemini-model:gemini-2.5-flash}") String model) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.apiKey = apiKey;
        this.model = model;
    }

    public String generate(Franchise franchise, Map<String, Object> averages) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Gemini API 키가 백엔드에 설정되지 않았습니다.");
        }

        try {
            String endpoint = "https://generativelanguage.googleapis.com/v1beta/models/"
                    + URLEncoder.encode(model, StandardCharsets.UTF_8)
                    + ":generateContent?key="
                    + URLEncoder.encode(apiKey, StandardCharsets.UTF_8);
            String body = objectMapper.writeValueAsString(Map.of(
                    "contents", List.of(Map.of(
                            "parts", List.of(Map.of("text", buildPrompt(franchise, averages)))
                    ))
            ));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(45))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Gemini API 호출 실패: HTTP " + response.statusCode());
            }

            return extractText(response.body());
        } catch (IOException ex) {
            throw new IllegalStateException("Gemini API 응답을 처리하지 못했습니다.", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Gemini API 호출이 중단되었습니다.", ex);
        }
    }

    private String extractText(String responseBody) throws IOException {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode textNode = root.path("candidates").path(0).path("content").path("parts").path(0).path("text");
        if (textNode.isMissingNode() || textNode.asText().isBlank()) {
            throw new IllegalStateException("Gemini API 응답에 분석 결과가 없습니다.");
        }

        return textNode.asText();
    }

    private String buildPrompt(Franchise franchise, Map<String, Object> averages) {
        List<MonthlySales> monthlySales = franchise.getMonthlySales();
        MonthlySales latest = monthlySales.get(monthlySales.size() - 1);
        MonthlySales previous = monthlySales.size() >= 2 ? monthlySales.get(monthlySales.size() - 2) : null;
        double salesGrowthRate = previous == null ? 0 : growthRate(latest.getSales(), previous.getSales());
        double txCountGrowthRate = previous == null ? 0 : growthRate(latest.getTxCount(), previous.getTxCount());
        double avgTicketGrowthRate = previous == null ? 0 : growthRate(latest.getAvgTicket(), previous.getAvgTicket());
        String monthlySalesSummary = monthlySales.stream()
                .map(item -> "- %s: 매출 %,d원, 거래 %,d건, 객단가 %,d원".formatted(
                        item.getMonth(),
                        item.getSales(),
                        item.getTxCount(),
                        item.getAvgTicket()))
                .reduce("", (left, right) -> left + right + "\n");

        long latestIndustryAverage = latestAverage(averages, "industryAverages", franchise.getIndustry());
        long latestRegionAverage = latestAverage(averages, "regionAverages", franchise.getRegion());
        double industryGapRate = latestIndustryAverage == 0 ? 0 : growthRate(latest.getSales(), latestIndustryAverage);
        double regionGapRate = latestRegionAverage == 0 ? 0 : growthRate(latest.getSales(), latestRegionAverage);
        String salesStatus = salesGrowthRate >= 5 ? "상승" : salesGrowthRate <= -5 ? "하락" : "보합";

        return """
                당신은 브랜드 본사와 영업 담당자를 위한 지도 기반 가맹점 매출 모니터링 플랫폼의 데이터 분석가입니다.
                점주에게 직접 조언하는 표현은 피하고, 본사 관리자와 담당 영업사원이 관리 우선순위를 판단할 수 있게 작성해주세요.
                데이터로 확인되지 않는 외부 요인은 단정하지 말고 가능성 또는 확인 필요로 표현해주세요.

                [가맹점 정보]
                - 매장명: %s
                - 업종: %s
                - 지역: %s
                - 주소: %s
                - 최근 월 매출: %,d원
                - 최근 월 결제 건수: %,d건
                - 평균 객단가: %,d원
                - 전월 대비 매출 변화율: %.1f%%
                - 전월 대비 거래 건수 변화율: %.1f%%
                - 전월 대비 객단가 변화율: %.1f%%
                - 현재 매출 상태 분류: %s
                - 시스템 위험 등급: %s
                - 시스템 점검 사유: %s

                [월별 이력]
                %s
                [비교 지표]
                - 동일 업종 평균 월 매출: %,d원
                - 동일 지역 평균 월 매출: %,d원
                - 동일 업종 평균 대비 차이: %.1f%%
                - 동일 지역 평균 대비 차이: %.1f%%

                출력 형식:
                **AI 운영 인사이트**
                - 상태 요약:
                - 비교 위치:
                - 원인 후보:
                - 이상 징후:
                - 모니터링 포인트:
                - 관리 우선순위:
                - 운영 태그:

                전체 답변은 500~700자 내외로 작성하고, 지표 기반 판단을 우선해주세요.
                """.formatted(
                franchise.getName(),
                franchise.getIndustry(),
                franchise.getRegion(),
                franchise.getAddress(),
                latest.getSales(),
                latest.getTxCount(),
                latest.getAvgTicket(),
                salesGrowthRate,
                txCountGrowthRate,
                avgTicketGrowthRate,
                salesStatus,
                franchise.getRiskLevel(),
                franchise.getRiskSummary(),
                monthlySalesSummary,
                latestIndustryAverage,
                latestRegionAverage,
                industryGapRate,
                regionGapRate);
    }

    @SuppressWarnings("unchecked")
    private long latestAverage(Map<String, Object> averages, String groupKey, String groupName) {
        Object groupMapNode = averages.get(groupKey);
        if (!(groupMapNode instanceof Map<?, ?> groupMap)) {
            return 0;
        }

        Object groupNode = groupMap.get(groupName);
        if (!(groupNode instanceof Map<?, ?> group)) {
            return 0;
        }

        Object salesNode = group.get("monthlySales");
        if (!(salesNode instanceof List<?> monthlySales) || monthlySales.isEmpty()) {
            return 0;
        }

        Object latestNode = monthlySales.get(monthlySales.size() - 1);
        if (!(latestNode instanceof Map<?, ?> latest)) {
            return 0;
        }

        Object sales = latest.get("sales");
        return sales instanceof Number number ? number.longValue() : 0;
    }

    private double growthRate(Number latest, Number previous) {
        if (latest == null || previous == null || previous.doubleValue() == 0) {
            return 0;
        }

        return ((latest.doubleValue() - previous.doubleValue()) / previous.doubleValue()) * 100;
    }
}
