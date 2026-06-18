package com.example.merchant.domain;

import java.util.List;

public class MerchantAlert {
    private String merchantId;
    private String merchantName;
    private String industry;
    private String region;
    private String riskLevel;
    private int priorityScore;
    private String summary;
    private List<String> reasons;
    private List<String> tags;
    private String latestMonth;
    private long latestSales;
    private double salesGrowthRate;
    private double txCountGrowthRate;
    private double avgTicketGrowthRate;

    public String getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(String merchantId) {
        this.merchantId = merchantId;
    }

    public String getMerchantName() {
        return merchantName;
    }

    public void setMerchantName(String merchantName) {
        this.merchantName = merchantName;
    }

    public String getIndustry() {
        return industry;
    }

    public void setIndustry(String industry) {
        this.industry = industry;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public int getPriorityScore() {
        return priorityScore;
    }

    public void setPriorityScore(int priorityScore) {
        this.priorityScore = priorityScore;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<String> getReasons() {
        return reasons;
    }

    public void setReasons(List<String> reasons) {
        this.reasons = reasons;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public String getLatestMonth() {
        return latestMonth;
    }

    public void setLatestMonth(String latestMonth) {
        this.latestMonth = latestMonth;
    }

    public long getLatestSales() {
        return latestSales;
    }

    public void setLatestSales(long latestSales) {
        this.latestSales = latestSales;
    }

    public double getSalesGrowthRate() {
        return salesGrowthRate;
    }

    public void setSalesGrowthRate(double salesGrowthRate) {
        this.salesGrowthRate = salesGrowthRate;
    }

    public double getTxCountGrowthRate() {
        return txCountGrowthRate;
    }

    public void setTxCountGrowthRate(double txCountGrowthRate) {
        this.txCountGrowthRate = txCountGrowthRate;
    }

    public double getAvgTicketGrowthRate() {
        return avgTicketGrowthRate;
    }

    public void setAvgTicketGrowthRate(double avgTicketGrowthRate) {
        this.avgTicketGrowthRate = avgTicketGrowthRate;
    }
}
