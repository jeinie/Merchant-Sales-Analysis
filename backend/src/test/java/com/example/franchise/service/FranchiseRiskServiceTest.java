package com.example.franchise.service;

import com.example.franchise.domain.Franchise;
import com.example.franchise.domain.FranchiseAlert;
import com.example.franchise.domain.MonthlySales;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FranchiseRiskServiceTest {
    private final FranchiseRiskService riskService = new FranchiseRiskService();

    @Test
    void classifiesLargeSalesDropAsCheckRequired() {
        Franchise franchise = franchise("F001", sales("2026-04", 1_000_000, 100, 10_000), sales("2026-05", 820_000, 82, 10_000));

        riskService.enrich(List.of(franchise));

        assertThat(franchise.getRiskLevel()).isEqualTo("CHECK_REQUIRED");
        assertThat(franchise.getPriorityScore()).isGreaterThanOrEqualTo(80);
        assertThat(franchise.getAlertTags()).contains("매출 급락", "거래 감소");
    }

    @Test
    void returnsOnlyNonNormalAlertsSortedByPriority() {
        Franchise normal = franchise("F001", sales("2026-04", 1_000_000, 100, 10_000), sales("2026-05", 1_030_000, 103, 10_000));
        Franchise caution = franchise("F002", sales("2026-04", 1_000_000, 100, 10_000), sales("2026-05", 880_000, 90, 9_777));
        Franchise checkRequired = franchise("F003", sales("2026-04", 1_000_000, 100, 10_000), sales("2026-05", 700_000, 70, 10_000));

        List<FranchiseAlert> alerts = riskService.alerts(List.of(normal, caution, checkRequired));

        assertThat(alerts).extracting(FranchiseAlert::getFranchiseId)
                .containsExactly("F003", "F002");
    }

    private Franchise franchise(String id, MonthlySales... monthlySales) {
        Franchise franchise = new Franchise();
        franchise.setId(id);
        franchise.setName("가맹점 " + id);
        franchise.setRegion("서울 강남구");
        franchise.setIndustry("카페");
        franchise.setMonthlySales(List.of(monthlySales));
        return franchise;
    }

    private MonthlySales sales(String month, long sales, int txCount, int avgTicket) {
        MonthlySales monthlySales = new MonthlySales();
        monthlySales.setMonth(month);
        monthlySales.setSales(sales);
        monthlySales.setTxCount(txCount);
        monthlySales.setAvgTicket(avgTicket);
        return monthlySales;
    }
}
