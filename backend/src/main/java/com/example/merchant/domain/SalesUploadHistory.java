package com.example.merchant.domain;

import java.time.Instant;

public class SalesUploadHistory {
    private Long id;
    private String fileName;
    private String uploadedBy;
    private String uploadedByName;
    private Integer totalRows;
    private Integer appliedRows;
    private Integer warningRows;
    private String status;
    private Instant createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(String uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public String getUploadedByName() {
        return uploadedByName;
    }

    public void setUploadedByName(String uploadedByName) {
        this.uploadedByName = uploadedByName;
    }

    public Integer getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(Integer totalRows) {
        this.totalRows = totalRows;
    }

    public Integer getAppliedRows() {
        return appliedRows;
    }

    public void setAppliedRows(Integer appliedRows) {
        this.appliedRows = appliedRows;
    }

    public Integer getWarningRows() {
        return warningRows;
    }

    public void setWarningRows(Integer warningRows) {
        this.warningRows = warningRows;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
