package com.example.merchant.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;
import java.util.Map;

public class User {
    private String id;
    @JsonIgnore
    private String passwordHash;
    private String name;
    private String role;
    private List<String> assignedMerchantIds;
    private Map<String, Object> permissions;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public List<String> getAssignedMerchantIds() {
        return assignedMerchantIds;
    }

    public void setAssignedMerchantIds(List<String> assignedMerchantIds) {
        this.assignedMerchantIds = assignedMerchantIds;
    }

    public Map<String, Object> getPermissions() {
        return permissions;
    }

    public void setPermissions(Map<String, Object> permissions) {
        this.permissions = permissions;
    }
}
