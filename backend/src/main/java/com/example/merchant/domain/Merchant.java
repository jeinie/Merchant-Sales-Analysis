package com.example.merchant.domain;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

public class Merchant {
    private String id;
    private String name;
    private String industry;
    private String region;
    private String address;
    private Double latitude;
    private Double longitude;
    private String locationStatus;
    private Instant geocodedAt;
    private String geocodeSource;
    private String locationNote;
    private String operationalStatus;
    private LocalDateTime closedAt;
    private String closureNote;
    private String riskLevel;
    private Integer priorityScore;
    private String riskSummary;
    private List<String> alertTags;
    private List<String> alertReasons;
    
    // For nesting the monthly sales data
    private List<MonthlySales> monthlySales;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getLocationStatus() {
        return locationStatus;
    }

    public void setLocationStatus(String locationStatus) {
        this.locationStatus = locationStatus;
    }

    public Instant getGeocodedAt() {
        return geocodedAt;
    }

    public void setGeocodedAt(Instant geocodedAt) {
        this.geocodedAt = geocodedAt;
    }

    public String getGeocodeSource() {
        return geocodeSource;
    }

    public void setGeocodeSource(String geocodeSource) {
        this.geocodeSource = geocodeSource;
    }

    public String getLocationNote() {
        return locationNote;
    }

    public void setLocationNote(String locationNote) {
        this.locationNote = locationNote;
    }

    public String getOperationalStatus() {
        return operationalStatus;
    }

    public void setOperationalStatus(String operationalStatus) {
        this.operationalStatus = operationalStatus;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(LocalDateTime closedAt) {
        this.closedAt = closedAt;
    }

    public String getClosureNote() {
        return closureNote;
    }

    public void setClosureNote(String closureNote) {
        this.closureNote = closureNote;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public Integer getPriorityScore() {
        return priorityScore;
    }

    public void setPriorityScore(Integer priorityScore) {
        this.priorityScore = priorityScore;
    }

    public String getRiskSummary() {
        return riskSummary;
    }

    public void setRiskSummary(String riskSummary) {
        this.riskSummary = riskSummary;
    }

    public List<String> getAlertTags() {
        return alertTags;
    }

    public void setAlertTags(List<String> alertTags) {
        this.alertTags = alertTags;
    }

    public List<String> getAlertReasons() {
        return alertReasons;
    }

    public void setAlertReasons(List<String> alertReasons) {
        this.alertReasons = alertReasons;
    }

    public List<MonthlySales> getMonthlySales() {
        return monthlySales;
    }

    public void setMonthlySales(List<MonthlySales> monthlySales) {
        this.monthlySales = monthlySales;
    }
}
