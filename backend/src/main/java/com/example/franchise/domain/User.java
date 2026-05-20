package com.example.franchise.domain;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class User {
    private String id;
    private String password;
    private String name;
    private String role;
    private List<String> assignedFranchiseIds;
    private Map<String, Object> permissions;
}
