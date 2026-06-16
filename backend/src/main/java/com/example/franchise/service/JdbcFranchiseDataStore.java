package com.example.franchise.service;

import com.example.franchise.domain.AiInsightHistory;
import com.example.franchise.domain.Franchise;
import com.example.franchise.domain.MonthlySales;
import com.example.franchise.domain.User;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Profile("gcp")
public class JdbcFranchiseDataStore implements FranchiseDataStore {

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedJdbcTemplate;
    private final PasswordHasher passwordHasher;

    public JdbcFranchiseDataStore(
            JdbcTemplate jdbcTemplate,
            NamedParameterJdbcTemplate namedJdbcTemplate,
            PasswordHasher passwordHasher) {
        this.jdbcTemplate = jdbcTemplate;
        this.namedJdbcTemplate = namedJdbcTemplate;
        this.passwordHasher = passwordHasher;
    }

    @Override
    public List<User> getPublicUsers() {
        return jdbcTemplate.query("""
                        SELECT id, password_hash, name, role, can_use_ai
                        FROM users
                        ORDER BY id
                        """, this::mapStoredUser)
                .stream()
                .map(this::publicUser)
                .collect(Collectors.toList());
    }

    @Override
    public List<User> getSalesUsers() {
        return jdbcTemplate.query("""
                        SELECT id, password_hash, name, role, can_use_ai
                        FROM users
                        WHERE role = 'SALES'
                        ORDER BY id
                        """, this::mapStoredUser)
                .stream()
                .map(this::publicUser)
                .collect(Collectors.toList());
    }

    @Override
    public User login(String id, String password) {
        User storedUser = findStoredUserById(id);
        if (storedUser == null || !passwordHasher.matches(password, storedUser.getPasswordHash())) {
            return null;
        }

        return publicUser(storedUser);
    }

    @Override
    public User findPublicUserById(String id) {
        User storedUser = findStoredUserById(id);
        return storedUser == null ? null : publicUser(storedUser);
    }

    @Override
    public List<Franchise> getFranchises(String userId, String role) {
        if (userId == null || userId.isBlank() || "ADMIN".equalsIgnoreCase(role)) {
            return loadAllFranchises();
        }

        List<String> assignedIds = loadAssignedFranchiseIds(userId, role);
        if (assignedIds.isEmpty()) {
            return List.of();
        }

        return loadFranchisesByIds(assignedIds);
    }

    @Override
    public Map<String, Object> getAverages() {
        List<Franchise> franchises = loadAllFranchises();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("industryAverages", calculateAverages(franchises, Franchise::getIndustry));
        response.put("regionAverages", calculateAverages(franchises, Franchise::getRegion));
        return response;
    }

    @Override
    public List<AiInsightHistory> getAiInsights(String franchiseId) {
        return jdbcTemplate.query("""
                        SELECT h.id, h.franchise_id, h.created_by, u.name AS created_by_name,
                               h.sales_month, h.risk_level, h.summary, h.content, h.tags, h.created_at
                        FROM ai_insight_histories h
                        JOIN users u ON u.id = h.created_by
                        WHERE h.franchise_id = ?
                        ORDER BY h.created_at DESC, h.id DESC
                        """,
                this::mapAiInsightHistory,
                franchiseId);
    }

    @Override
    public AiInsightHistory getLatestAiInsight(String franchiseId) {
        try {
            return jdbcTemplate.queryForObject("""
                            SELECT h.id, h.franchise_id, h.created_by, u.name AS created_by_name,
                                   h.sales_month, h.risk_level, h.summary, h.content, h.tags, h.created_at
                            FROM ai_insight_histories h
                            JOIN users u ON u.id = h.created_by
                            WHERE h.franchise_id = ?
                            ORDER BY h.created_at DESC, h.id DESC
                            LIMIT 1
                            """,
                    this::mapAiInsightHistory,
                    franchiseId);
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }

    @Override
    public AiInsightHistory saveAiInsight(
            String franchiseId,
            String createdBy,
            String salesMonth,
            String riskLevel,
            String summary,
            String content,
            List<String> tags) {
        jdbcTemplate.update("""
                        INSERT INTO ai_insight_histories
                            (franchise_id, created_by, sales_month, risk_level, summary, content, tags)
                        VALUES (?, ?, ?, ?, ?, ?, ?)
                        """,
                franchiseId,
                createdBy,
                salesMonth,
                riskLevel,
                summary,
                content,
                String.join(",", tags));

        return getLatestAiInsight(franchiseId);
    }

    @Override
    @Transactional
    public void assignManager(String franchiseId, String managerId) {
        if (!existsById("franchises", franchiseId)) {
            throw new IllegalArgumentException("존재하지 않는 가맹점입니다.");
        }

        if (managerId != null && !managerId.isBlank() && !existsSalesUser(managerId)) {
            throw new IllegalArgumentException("존재하지 않는 영업사원입니다.");
        }

        jdbcTemplate.update(
                "DELETE FROM user_franchise_assignments WHERE franchise_id = ?",
                franchiseId);

        if (managerId != null && !managerId.isBlank()) {
            jdbcTemplate.update("""
                            INSERT INTO user_franchise_assignments (user_id, franchise_id)
                            VALUES (?, ?)
                            """,
                    managerId,
                    franchiseId);
        }
    }

    @Override
    public void toggleAi(String userId, boolean canUseAI) {
        int updated = jdbcTemplate.update(
                "UPDATE users SET can_use_ai = ? WHERE id = ?",
                canUseAI,
                userId);

        if (updated == 0) {
            throw new IllegalArgumentException("존재하지 않는 사용자입니다.");
        }
    }

    private User findStoredUserById(String id) {
        try {
            return jdbcTemplate.queryForObject("""
                            SELECT id, password_hash, name, role, can_use_ai
                            FROM users
                            WHERE id = ?
                            """,
                    this::mapStoredUser,
                    id);
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }

    private List<Franchise> loadAllFranchises() {
        List<Franchise> franchises = jdbcTemplate.query("""
                SELECT id, name, industry, region, address, latitude, longitude
                FROM franchises
                ORDER BY id
                """, this::mapFranchise);
        return attachMonthlySales(franchises);
    }

    private List<Franchise> loadFranchisesByIds(List<String> franchiseIds) {
        List<Franchise> franchises = namedJdbcTemplate.query("""
                        SELECT id, name, industry, region, address, latitude, longitude
                        FROM franchises
                        WHERE id IN (:ids)
                        ORDER BY id
                        """,
                Map.of("ids", franchiseIds),
                this::mapFranchise);
        return attachMonthlySales(franchises);
    }

    private List<Franchise> attachMonthlySales(List<Franchise> franchises) {
        if (franchises.isEmpty()) {
            return franchises;
        }

        Map<String, Franchise> franchiseById = franchises.stream()
                .collect(Collectors.toMap(
                        Franchise::getId,
                        Function.identity(),
                        (left, right) -> left,
                        LinkedHashMap::new));

        namedJdbcTemplate.query("""
                        SELECT id, franchise_id, sales_month, sales, tx_count, avg_ticket
                        FROM monthly_sales
                        WHERE franchise_id IN (:ids)
                        ORDER BY franchise_id, sales_month
                        """,
                Map.of("ids", franchiseById.keySet()),
                rs -> {
                    Franchise franchise = franchiseById.get(rs.getString("franchise_id"));
                    if (franchise != null) {
                        franchise.getMonthlySales().add(mapMonthlySales(rs));
                    }
                });

        return franchises;
    }

    private List<String> loadAssignedFranchiseIds(String userId, String role) {
        if (!"SALES".equalsIgnoreCase(role)) {
            return List.of();
        }

        return jdbcTemplate.queryForList("""
                SELECT franchise_id
                FROM user_franchise_assignments
                WHERE user_id = ?
                ORDER BY franchise_id
                """, String.class, userId);
    }

    private boolean existsById(String tableName, String id) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + tableName + " WHERE id = ?",
                Integer.class,
                id);
        return count != null && count > 0;
    }

    private boolean existsSalesUser(String userId) {
        Integer count = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*)
                        FROM users
                        WHERE id = ? AND role = 'SALES'
                        """,
                Integer.class,
                userId);
        return count != null && count > 0;
    }

    private User mapStoredUser(ResultSet rs, int rowNum) throws SQLException {
        User user = new User();
        user.setId(rs.getString("id"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setName(rs.getString("name"));
        user.setRole(rs.getString("role"));
        user.setPermissions(Map.of("canUseAI", rs.getBoolean("can_use_ai")));
        return user;
    }

    private Franchise mapFranchise(ResultSet rs, int rowNum) throws SQLException {
        Franchise franchise = new Franchise();
        franchise.setId(rs.getString("id"));
        franchise.setName(rs.getString("name"));
        franchise.setIndustry(rs.getString("industry"));
        franchise.setRegion(rs.getString("region"));
        franchise.setAddress(rs.getString("address"));
        franchise.setLatitude(nullableDouble(rs, "latitude"));
        franchise.setLongitude(nullableDouble(rs, "longitude"));
        franchise.setMonthlySales(new ArrayList<>());
        return franchise;
    }

    private MonthlySales mapMonthlySales(ResultSet rs) throws SQLException {
        MonthlySales monthlySales = new MonthlySales();
        monthlySales.setId(rs.getLong("id"));
        monthlySales.setFranchiseId(rs.getString("franchise_id"));
        monthlySales.setMonth(rs.getString("sales_month"));
        monthlySales.setSales(rs.getLong("sales"));
        monthlySales.setTxCount(rs.getInt("tx_count"));
        monthlySales.setAvgTicket(rs.getInt("avg_ticket"));
        return monthlySales;
    }

    private AiInsightHistory mapAiInsightHistory(ResultSet rs, int rowNum) throws SQLException {
        AiInsightHistory history = new AiInsightHistory();
        history.setId(rs.getLong("id"));
        history.setFranchiseId(rs.getString("franchise_id"));
        history.setCreatedBy(rs.getString("created_by"));
        history.setCreatedByName(rs.getString("created_by_name"));
        history.setSalesMonth(rs.getString("sales_month"));
        history.setRiskLevel(rs.getString("risk_level"));
        history.setSummary(rs.getString("summary"));
        history.setContent(rs.getString("content"));
        history.setTags(splitTags(rs.getString("tags")));

        Timestamp createdAt = rs.getTimestamp("created_at");
        history.setCreatedAt(createdAt == null ? null : createdAt.toInstant());
        return history;
    }

    private List<String> splitTags(String tags) {
        if (tags == null || tags.isBlank()) {
            return List.of();
        }

        return List.of(tags.split(",")).stream()
                .map(String::trim)
                .filter(tag -> !tag.isBlank())
                .toList();
    }

    private Double nullableDouble(ResultSet rs, String columnName) throws SQLException {
        double value = rs.getDouble(columnName);
        return rs.wasNull() ? null : value;
    }

    private User publicUser(User user) {
        User copy = new User();
        copy.setId(user.getId());
        copy.setName(user.getName());
        copy.setRole(user.getRole());
        copy.setAssignedFranchiseIds(loadAssignedFranchiseIds(user.getId(), user.getRole()));
        copy.setPermissions(new HashMap<>(user.getPermissions()));
        return copy;
    }

    private Map<String, Map<String, Object>> calculateAverages(
            List<Franchise> franchises,
            Function<Franchise, String> groupBy) {
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
