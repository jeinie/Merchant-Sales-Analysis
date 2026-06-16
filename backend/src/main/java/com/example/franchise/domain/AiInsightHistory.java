package com.example.franchise.domain;

import java.time.Instant;
import java.util.List;

public class AiInsightHistory {
    private Long id;
    private String franchiseId;
    private String createdBy;
    private String createdByName;
    private String salesMonth;
    private String riskLevel;
    private String summary;
    private String content;
    private List<String> tags;
    private Instant createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFranchiseId() {
        return franchiseId;
    }

    public void setFranchiseId(String franchiseId) {
        this.franchiseId = franchiseId;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getCreatedByName() {
        return createdByName;
    }

    public void setCreatedByName(String createdByName) {
        this.createdByName = createdByName;
    }

    public String getSalesMonth() {
        return salesMonth;
    }

    public void setSalesMonth(String salesMonth) {
        this.salesMonth = salesMonth;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
