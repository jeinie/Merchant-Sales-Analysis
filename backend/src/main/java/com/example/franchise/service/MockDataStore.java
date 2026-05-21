package com.example.franchise.service;

import com.example.franchise.domain.Franchise;
import com.example.franchise.domain.MonthlySales;
import com.example.franchise.domain.User;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class MockDataStore {

    private final PasswordHasher passwordHasher;
    private final List<Franchise> franchises = new ArrayList<>();
    private final List<User> users = new ArrayList<>();

    public MockDataStore(PasswordHasher passwordHasher) {
        this.passwordHasher = passwordHasher;
        seedUsers();
        seedFranchises();
    }

    public synchronized List<User> getPublicUsers() {
        return users.stream()
                .map(this::publicUser)
                .collect(Collectors.toList());
    }

    public synchronized List<User> getSalesUsers() {
        return users.stream()
                .filter(user -> "SALES".equals(user.getRole()))
                .map(this::publicUser)
                .collect(Collectors.toList());
    }

    public synchronized User login(String id, String password) {
        return users.stream()
                .filter(user -> user.getId().equals(id) && passwordHasher.matches(password, user.getPasswordHash()))
                .findFirst()
                .map(this::publicUser)
                .orElse(null);
    }

    public synchronized User findPublicUserById(String id) {
        return users.stream()
                .filter(user -> user.getId().equals(id))
                .findFirst()
                .map(this::publicUser)
                .orElse(null);
    }

    public synchronized List<Franchise> getFranchises(String userId, String role) {
        if (userId == null || userId.isBlank() || "ADMIN".equalsIgnoreCase(role)) {
            return franchises.stream()
                    .map(this::copyFranchise)
                    .collect(Collectors.toList());
        }

        User user = users.stream()
                .filter(item -> item.getId().equals(userId))
                .findFirst()
                .orElse(null);

        List<String> assignedIds = user == null || user.getAssignedFranchiseIds() == null
                ? List.of()
                : user.getAssignedFranchiseIds();

        return franchises.stream()
                .filter(franchise -> assignedIds.contains(franchise.getId()))
                .map(this::copyFranchise)
                .collect(Collectors.toList());
    }

    public synchronized Map<String, Object> getAverages() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("industryAverages", calculateAverages(Franchise::getIndustry));
        response.put("regionAverages", calculateAverages(Franchise::getRegion));
        return response;
    }

    public synchronized void assignManager(String franchiseId, String managerId) {
        boolean franchiseExists = franchises.stream().anyMatch(franchise -> franchise.getId().equals(franchiseId));
        if (!franchiseExists) {
            throw new IllegalArgumentException("존재하지 않는 가맹점입니다.");
        }

        if (managerId != null && !managerId.isBlank()) {
            boolean managerExists = users.stream()
                    .anyMatch(user -> user.getId().equals(managerId) && "SALES".equals(user.getRole()));
            if (!managerExists) {
                throw new IllegalArgumentException("존재하지 않는 영업사원입니다.");
            }
        }

        for (User user : users) {
            if (!"SALES".equals(user.getRole())) {
                continue;
            }

            List<String> assignedIds = user.getAssignedFranchiseIds() == null
                    ? new ArrayList<>()
                    : new ArrayList<>(user.getAssignedFranchiseIds());
            assignedIds.remove(franchiseId);
            if (user.getId().equals(managerId)) {
                assignedIds.add(franchiseId);
            }
            user.setAssignedFranchiseIds(assignedIds);
        }
    }

    public synchronized void toggleAi(String userId, boolean canUseAI) {
        User user = users.stream()
                .filter(item -> item.getId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        Map<String, Object> permissions = new HashMap<>(user.getPermissions());
        permissions.put("canUseAI", canUseAI);
        user.setPermissions(permissions);
    }

    private void seedUsers() {
        users.add(user("admin", "1234", "시스템 관리자", "ADMIN", null, true));
        users.add(user("sales_user", "1234", "김영업 사원", "SALES", List.of("F001", "F002", "F003", "F004", "F005"), true));
        users.add(user("sales_user2", "1234", "이영업 사원", "SALES", List.of("F006", "F007", "F008", "F009", "F010"), true));
    }

    private void seedFranchises() {
        franchises.add(franchise(
                "F001",
                "강남역 1호점",
                "카페",
                "서울 강남구",
                "서울 강남구 강남대로 396",
                37.4979,
                127.0276,
                List.of(
                        sale(1L, "F001", "2023-01", 15000000L, 1500, 10000),
                        sale(2L, "F001", "2023-02", 16500000L, 1600, 10312),
                        sale(3L, "F001", "2023-03", 18000000L, 1800, 10000)
                )));

        franchises.add(franchise(
                "F002",
                "홍대입구점",
                "음식점",
                "서울 마포구",
                "서울 마포구 양화로 160",
                37.5572,
                126.9245,
                List.of(
                        sale(4L, "F002", "2023-01", 25000000L, 800, 31250),
                        sale(5L, "F002", "2023-02", 23000000L, 750, 30666),
                        sale(6L, "F002", "2023-03", 28000000L, 900, 31111)
                )));

        franchises.add(franchise(
                "F003",
                "여의도 금융타운점",
                "카페",
                "서울 영등포구",
                "서울 영등포구 여의대로 108",
                37.5259,
                126.9286,
                List.of(
                        sale(7L, "F003", "2023-01", 22000000L, 3000, 7333),
                        sale(8L, "F003", "2023-02", 20000000L, 2800, 7142),
                        sale(9L, "F003", "2023-03", 25000000L, 3200, 7812)
                )));

        franchises.add(franchise(
                "F004",
                "성수 카페거리점",
                "카페",
                "서울 성동구",
                "서울 성동구 연무장길 14",
                37.5438,
                127.0565,
                List.of(
                        sale(10L, "F004", "2023-01", 18000000L, 1200, 15000),
                        sale(11L, "F004", "2023-02", 20000000L, 1300, 15384),
                        sale(12L, "F004", "2023-03", 20400000L, 1320, 15454)
                )));

        franchises.add(franchise(
                "F005",
                "테헤란 점심특화점",
                "음식점",
                "서울 강남구",
                "서울 강남구 테헤란로 152",
                37.5013,
                127.0396,
                List.of(
                        sale(13L, "F005", "2023-01", 17000000L, 850, 20000),
                        sale(14L, "F005", "2023-02", 19000000L, 930, 20430),
                        sale(15L, "F005", "2023-03", 16000000L, 780, 20512)
                )));

        franchises.add(franchise(
                "F006",
                "합정 브런치점",
                "음식점",
                "서울 마포구",
                "서울 마포구 독막로 10",
                37.5496,
                126.9139,
                List.of(
                        sale(16L, "F006", "2023-01", 26000000L, 1040, 25000),
                        sale(17L, "F006", "2023-02", 26500000L, 1060, 25000),
                        sale(18L, "F006", "2023-03", 27000000L, 1080, 25000)
                )));

        franchises.add(franchise(
                "F007",
                "샛강 테이크아웃점",
                "카페",
                "서울 영등포구",
                "서울 영등포구 의사당대로 83",
                37.5184,
                126.9317,
                List.of(
                        sale(19L, "F007", "2023-01", 19000000L, 2600, 7307),
                        sale(20L, "F007", "2023-02", 18000000L, 2500, 7200),
                        sale(21L, "F007", "2023-03", 15000000L, 2100, 7142)
                )));

        franchises.add(franchise(
                "F008",
                "성수 로스터리점",
                "카페",
                "서울 성동구",
                "서울 성동구 성수이로 87",
                37.5446,
                127.0557,
                List.of(
                        sale(22L, "F008", "2023-01", 14000000L, 950, 14736),
                        sale(23L, "F008", "2023-02", 15500000L, 1050, 14761),
                        sale(24L, "F008", "2023-03", 19000000L, 1250, 15200)
                )));

        franchises.add(franchise(
                "F009",
                "상암 오피스푸드점",
                "음식점",
                "서울 마포구",
                "서울 마포구 월드컵북로 396",
                37.5796,
                126.8896,
                List.of(
                        sale(25L, "F009", "2023-01", 30000000L, 1200, 25000),
                        sale(26L, "F009", "2023-02", 30600000L, 1220, 25081),
                        sale(27L, "F009", "2023-03", 31200000L, 1240, 25161)
                )));

        franchises.add(franchise(
                "F010",
                "선릉 라운지카페",
                "카페",
                "서울 강남구",
                "서울 강남구 선릉로 428",
                37.5045,
                127.0490,
                List.of(
                        sale(28L, "F010", "2023-01", 28000000L, 1900, 14736),
                        sale(29L, "F010", "2023-02", 27000000L, 1850, 14594),
                        sale(30L, "F010", "2023-03", 22000000L, 1500, 14666)
                )));
    }

    private User user(
            String id,
            String password,
            String name,
            String role,
            List<String> assignedFranchiseIds,
            boolean canUseAI) {
        User user = new User();
        user.setId(id);
        user.setPasswordHash(passwordHasher.hash(password));
        user.setName(name);
        user.setRole(role);
        user.setAssignedFranchiseIds(assignedFranchiseIds == null ? null : new ArrayList<>(assignedFranchiseIds));
        user.setPermissions(Map.of("canUseAI", canUseAI));
        return user;
    }

    private Franchise franchise(
            String id,
            String name,
            String industry,
            String region,
            String address,
            Double latitude,
            Double longitude,
            List<MonthlySales> monthlySales) {
        Franchise franchise = new Franchise();
        franchise.setId(id);
        franchise.setName(name);
        franchise.setIndustry(industry);
        franchise.setRegion(region);
        franchise.setAddress(address);
        franchise.setLatitude(latitude);
        franchise.setLongitude(longitude);
        franchise.setMonthlySales(new ArrayList<>(monthlySales));
        return franchise;
    }

    private MonthlySales sale(
            Long id,
            String franchiseId,
            String month,
            Long sales,
            Integer txCount,
            Integer avgTicket) {
        MonthlySales monthlySales = new MonthlySales();
        monthlySales.setId(id);
        monthlySales.setFranchiseId(franchiseId);
        monthlySales.setMonth(month);
        monthlySales.setSales(sales);
        monthlySales.setTxCount(txCount);
        monthlySales.setAvgTicket(avgTicket);
        return monthlySales;
    }

    private User publicUser(User user) {
        User copy = new User();
        copy.setId(user.getId());
        copy.setName(user.getName());
        copy.setRole(user.getRole());
        copy.setAssignedFranchiseIds(user.getAssignedFranchiseIds() == null
                ? null
                : new ArrayList<>(user.getAssignedFranchiseIds()));
        copy.setPermissions(new HashMap<>(user.getPermissions()));
        return copy;
    }

    private Franchise copyFranchise(Franchise franchise) {
        Franchise copy = new Franchise();
        copy.setId(franchise.getId());
        copy.setName(franchise.getName());
        copy.setIndustry(franchise.getIndustry());
        copy.setRegion(franchise.getRegion());
        copy.setAddress(franchise.getAddress());
        copy.setLatitude(franchise.getLatitude());
        copy.setLongitude(franchise.getLongitude());
        copy.setMonthlySales(franchise.getMonthlySales().stream()
                .map(this::copySale)
                .collect(Collectors.toList()));
        return copy;
    }

    private MonthlySales copySale(MonthlySales monthlySales) {
        return sale(
                monthlySales.getId(),
                monthlySales.getFranchiseId(),
                monthlySales.getMonth(),
                monthlySales.getSales(),
                monthlySales.getTxCount(),
                monthlySales.getAvgTicket());
    }

    private Map<String, Map<String, Object>> calculateAverages(Function<Franchise, String> groupBy) {
        Map<String, Map<String, List<Long>>> salesByGroupAndMonth = new LinkedHashMap<>();

        for (Franchise franchise : franchises) {
            String groupName = groupBy.apply(franchise);
            Map<String, List<Long>> salesByMonth = salesByGroupAndMonth.computeIfAbsent(
                    groupName,
                    key -> new LinkedHashMap<>());

            for (MonthlySales monthlySales : franchise.getMonthlySales()) {
                salesByMonth.computeIfAbsent(monthlySales.getMonth(), key -> new ArrayList<>())
                        .add(monthlySales.getSales());
            }
        }

        Map<String, Map<String, Object>> averages = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, List<Long>>> groupEntry : salesByGroupAndMonth.entrySet()) {
            List<Map<String, Object>> monthlySales = new ArrayList<>();
            for (Map.Entry<String, List<Long>> monthEntry : groupEntry.getValue().entrySet()) {
                double average = monthEntry.getValue().stream()
                        .mapToLong(Long::longValue)
                        .average()
                        .orElse(0);

                Map<String, Object> salesNode = new LinkedHashMap<>();
                salesNode.put("month", monthEntry.getKey());
                salesNode.put("sales", Math.round(average));
                monthlySales.add(salesNode);
            }

            Map<String, Object> groupNode = new LinkedHashMap<>();
            groupNode.put("monthlySales", monthlySales);
            averages.put(groupEntry.getKey(), groupNode);
        }

        return averages;
    }
}
