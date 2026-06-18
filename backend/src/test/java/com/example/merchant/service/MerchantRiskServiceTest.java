package com.example.merchant.service;

import com.example.merchant.domain.Merchant;
import com.example.merchant.domain.MerchantAlert;
import com.example.merchant.domain.MonthlySales;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MerchantRiskServiceTest {
    private final MerchantRiskService riskService = new MerchantRiskService();

    @Test
    void classifiesLargeSalesDropAsCheckRequired() {
        Merchant merchant = merchant("M001", sales("2026-04", 1_000_000, 100, 10_000), sales("2026-05", 820_000, 82, 10_000));

        riskService.enrich(List.of(merchant));

        assertThat(merchant.getRiskLevel()).isEqualTo("CHECK_REQUIRED");
        assertThat(merchant.getPriorityScore()).isGreaterThanOrEqualTo(80);
        assertThat(merchant.getAlertTags()).contains("매출 급락", "거래 감소");
    }

    @Test
    void returnsOnlyNonNormalAlertsSortedByPriority() {
        Merchant normal = merchant("M001", sales("2026-04", 1_000_000, 100, 10_000), sales("2026-05", 1_030_000, 103, 10_000));
        Merchant caution = merchant("M002", sales("2026-04", 1_000_000, 100, 10_000), sales("2026-05", 880_000, 90, 9_777));
        Merchant checkRequired = merchant("M003", sales("2026-04", 1_000_000, 100, 10_000), sales("2026-05", 700_000, 70, 10_000));

        List<MerchantAlert> alerts = riskService.alerts(List.of(normal, caution, checkRequired));

        assertThat(alerts).extracting(MerchantAlert::getMerchantId)
                .containsExactly("M003", "M002");
    }

    private Merchant merchant(String id, MonthlySales... monthlySales) {
        Merchant merchant = new Merchant();
        merchant.setId(id);
        merchant.setName("가맹점 " + id);
        merchant.setRegion("서울 강남구");
        merchant.setIndustry("카페");
        merchant.setMonthlySales(List.of(monthlySales));
        return merchant;
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
