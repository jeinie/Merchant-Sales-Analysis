package com.example.merchant.service;

import com.example.merchant.domain.AiInsightHistory;
import com.example.merchant.domain.AssignmentHistory;
import com.example.merchant.domain.Merchant;
import com.example.merchant.domain.User;

import java.util.List;
import java.util.Map;

public interface MerchantDataStore {
    List<User> getPublicUsers();

    List<User> getSalesUsers();

    User login(String id, String password);

    User findPublicUserById(String id);

    List<Merchant> getMerchants(String userId, String role);

    Map<String, Object> getAverages();

    List<AiInsightHistory> getAiInsights(String merchantId);

    AiInsightHistory getLatestAiInsight(String merchantId);

    AiInsightHistory saveAiInsight(
            String merchantId,
            String createdBy,
            String salesMonth,
            String riskLevel,
            String summary,
            String content,
            String note,
            List<String> tags);

    AiInsightHistory updateAiInsightNote(Long insightId, String merchantId, String note);

    Merchant createMerchant(
            String name,
            String industry,
            String region,
            String address,
            Double latitude,
            Double longitude,
            String locationStatus,
            String geocodeSource,
            String locationNote,
            String managerId,
            String changedBy);

    Merchant updateMerchant(
            String merchantId,
            String name,
            String industry,
            String region,
            String address,
            Double latitude,
            Double longitude,
            String locationStatus,
            String geocodeSource,
            String locationNote);

    void updateMerchantStatus(String merchantId, String operationalStatus, String statusNote);

    void assignManager(String merchantId, String managerId, String changedBy, String changeReason);

    List<AssignmentHistory> getAssignmentHistories();

    Merchant updateMerchantLocation(
            String merchantId,
            Double latitude,
            Double longitude,
            String locationStatus,
            String geocodeSource,
            String locationNote);

    void toggleAi(String userId, boolean canUseAI);
}
