package com.example.franchise.service;

import com.example.franchise.domain.Franchise;
import com.example.franchise.domain.User;

import java.util.List;
import java.util.Map;

public interface FranchiseDataStore {
    List<User> getPublicUsers();

    List<User> getSalesUsers();

    User login(String id, String password);

    User findPublicUserById(String id);

    List<Franchise> getFranchises(String userId, String role);

    Map<String, Object> getAverages();

    void assignManager(String franchiseId, String managerId);

    void toggleAi(String userId, boolean canUseAI);
}
