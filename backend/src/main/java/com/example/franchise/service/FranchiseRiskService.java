package com.example.franchise.service;

import com.example.franchise.domain.Franchise;
import com.example.franchise.domain.FranchiseAlert;
import com.example.franchise.domain.MonthlySales;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class FranchiseRiskService {
    static final double SALES_CAUTION_DROP_RATE = -10.0;
    static final double SALES_CHECK_REQUIRED_DROP_RATE = -15.0;
    static final double SALES_SPIKE_RATE = 20.0;
    static final double TX_COUNT_DROP_RATE = -12.0;
    static final double AVG_TICKET_DROP_RATE = -10.0;

    public List<Franchise> enrich(List<Franchise> franchises) {
        for (Franchise franchise : franchises) {
            Assessment assessment = assess(franchise);
            franchise.setRiskLevel(assessment.riskLevel());
            franchise.setPriorityScore(assessment.priorityScore());
            franchise.setRiskSummary(assessment.summary());
            franchise.setAlertTags(assessment.tags());
            franchise.setAlertReasons(assessment.reasons());
        }

        return franchises;
    }

    public List<FranchiseAlert> alerts(List<Franchise> franchises) {
        return franchises.stream()
                .map(franchise -> toAlert(franchise, assess(franchise)))
                .filter(alert -> !"NORMAL".equals(alert.getRiskLevel()))
                .sorted(Comparator
                        .comparingInt(FranchiseAlert::getPriorityScore).reversed()
                        .thenComparing(FranchiseAlert::getFranchiseName))
                .toList();
    }

    Assessment assess(Franchise franchise) {
        List<MonthlySales> monthlySales = franchise.getMonthlySales();
        if (monthlySales == null || monthlySales.size() < 2) {
            return new Assessment(
                    "NORMAL",
                    0,
                    "비교 가능한 월별 매출 데이터가 부족합니다.",
                    List.of("데이터 부족"),
                    List.of());
        }

        MonthlySales latest = monthlySales.get(monthlySales.size() - 1);
        MonthlySales previous = monthlySales.get(monthlySales.size() - 2);
        double salesGrowthRate = growthRate(latest.getSales(), previous.getSales());
        double txCountGrowthRate = growthRate(latest.getTxCount(), previous.getTxCount());
        double avgTicketGrowthRate = growthRate(latest.getAvgTicket(), previous.getAvgTicket());

        List<String> reasons = new ArrayList<>();
        List<String> tags = new ArrayList<>();
        int priorityScore = 0;
        String riskLevel = "NORMAL";

        if (salesGrowthRate <= SALES_CHECK_REQUIRED_DROP_RATE) {
            riskLevel = "CHECK_REQUIRED";
            priorityScore += 80;
            tags.add("매출 급락");
            reasons.add(String.format("전월 대비 매출이 %.1f%% 하락해 점검 기준을 초과했습니다.", Math.abs(salesGrowthRate)));
        } else if (salesGrowthRate <= SALES_CAUTION_DROP_RATE) {
            riskLevel = "CAUTION";
            priorityScore += 55;
            tags.add("매출 하락");
            reasons.add(String.format("전월 대비 매출이 %.1f%% 하락해 주의 관찰이 필요합니다.", Math.abs(salesGrowthRate)));
        } else if (salesGrowthRate >= SALES_SPIKE_RATE) {
            riskLevel = "CAUTION";
            priorityScore += 45;
            tags.add("매출 급등");
            reasons.add(String.format("전월 대비 매출이 %.1f%% 상승해 일시적 급등 여부 확인이 필요합니다.", salesGrowthRate));
        }

        if (txCountGrowthRate <= TX_COUNT_DROP_RATE) {
            priorityScore += 15;
            tags.add("거래 감소");
            reasons.add(String.format("거래 건수가 전월 대비 %.1f%% 감소했습니다.", Math.abs(txCountGrowthRate)));
            if ("NORMAL".equals(riskLevel)) {
                riskLevel = "CAUTION";
            }
        }

        if (avgTicketGrowthRate <= AVG_TICKET_DROP_RATE) {
            priorityScore += 10;
            tags.add("객단가 감소");
            reasons.add(String.format("객단가가 전월 대비 %.1f%% 감소했습니다.", Math.abs(avgTicketGrowthRate)));
            if ("NORMAL".equals(riskLevel)) {
                riskLevel = "CAUTION";
            }
        }

        if (reasons.isEmpty()) {
            reasons.add("전월 대비 주요 지표가 설정된 주의 기준 안에 있습니다.");
            tags.add("정상 관찰");
        }

        String summary = switch (riskLevel) {
            case "CHECK_REQUIRED" -> "매출 하락 폭이 커 담당자 점검이 필요합니다.";
            case "CAUTION" -> "주요 지표 변동이 있어 주의 관찰이 필요합니다.";
            default -> "현재 기준에서는 정상 관찰 대상입니다.";
        };

        return new Assessment(
                riskLevel,
                Math.min(priorityScore, 100),
                summary,
                List.copyOf(tags),
                List.copyOf(reasons));
    }

    private FranchiseAlert toAlert(Franchise franchise, Assessment assessment) {
        List<MonthlySales> monthlySales = franchise.getMonthlySales();
        MonthlySales latest = monthlySales == null || monthlySales.isEmpty() ? null : monthlySales.get(monthlySales.size() - 1);
        MonthlySales previous = monthlySales == null || monthlySales.size() < 2 ? null : monthlySales.get(monthlySales.size() - 2);

        FranchiseAlert alert = new FranchiseAlert();
        alert.setFranchiseId(franchise.getId());
        alert.setFranchiseName(franchise.getName());
        alert.setIndustry(franchise.getIndustry());
        alert.setRegion(franchise.getRegion());
        alert.setRiskLevel(assessment.riskLevel());
        alert.setPriorityScore(assessment.priorityScore());
        alert.setSummary(assessment.summary());
        alert.setReasons(assessment.reasons());
        alert.setTags(assessment.tags());
        alert.setLatestMonth(latest == null ? "" : latest.getMonth());
        alert.setLatestSales(latest == null || latest.getSales() == null ? 0 : latest.getSales());
        alert.setSalesGrowthRate(latest == null || previous == null ? 0 : growthRate(latest.getSales(), previous.getSales()));
        alert.setTxCountGrowthRate(latest == null || previous == null ? 0 : growthRate(latest.getTxCount(), previous.getTxCount()));
        alert.setAvgTicketGrowthRate(latest == null || previous == null ? 0 : growthRate(latest.getAvgTicket(), previous.getAvgTicket()));
        return alert;
    }

    private double growthRate(Number latest, Number previous) {
        if (latest == null || previous == null || previous.doubleValue() == 0) {
            return 0;
        }

        return ((latest.doubleValue() - previous.doubleValue()) / previous.doubleValue()) * 100;
    }

    record Assessment(
            String riskLevel,
            int priorityScore,
            String summary,
            List<String> tags,
            List<String> reasons) {
    }
}
