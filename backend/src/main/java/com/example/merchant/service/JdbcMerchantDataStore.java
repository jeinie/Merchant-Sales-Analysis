package com.example.merchant.service;

import com.example.merchant.domain.AiInsightHistory;
import com.example.merchant.domain.AssignmentHistory;
import com.example.merchant.domain.Merchant;
import com.example.merchant.domain.MonthlySales;
import com.example.merchant.domain.User;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.BadSqlGrammarException;
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
import java.util.OptionalInt;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Profile("gcp")
public class JdbcMerchantDataStore implements MerchantDataStore {

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedJdbcTemplate;
    private final PasswordHasher passwordHasher;

    public JdbcMerchantDataStore(
            JdbcTemplate jdbcTemplate,
            NamedParameterJdbcTemplate namedJdbcTemplate,
            PasswordHasher passwordHasher) {
        this.jdbcTemplate = jdbcTemplate;
        this.namedJdbcTemplate = namedJdbcTemplate;
        this.passwordHasher = passwordHasher;
    }

    @PostConstruct
    void ensureAiInsightSchema() {
        ensureMerchantSchemaNames();
        ensureMerchantLocationColumns();
        ensureAssignmentHistorySchema();

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS ai_insight_histories (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    merchant_id VARCHAR(20) NOT NULL,
                    created_by VARCHAR(64) NOT NULL,
                    sales_month CHAR(7) NOT NULL,
                    risk_level VARCHAR(20) NOT NULL,
                    summary VARCHAR(500) NOT NULL,
                    content TEXT NOT NULL,
                    note TEXT,
                    tags VARCHAR(255),
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (id),
                    KEY idx_ai_insight_merchant_month (merchant_id, sales_month),
                    CONSTRAINT fk_ai_insight_merchant
                        FOREIGN KEY (merchant_id) REFERENCES merchant (id)
                        ON DELETE CASCADE,
                    CONSTRAINT fk_ai_insight_user
                        FOREIGN KEY (created_by) REFERENCES users (id)
                        ON DELETE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);

        try {
            jdbcTemplate.execute("ALTER TABLE ai_insight_histories ADD COLUMN note TEXT");
        } catch (BadSqlGrammarException | DuplicateKeyException ex) {
            // Existing databases may already have the column; the table definition above handles fresh schemas.
        }
    }

    private void ensureMerchantSchemaNames() {
        renameTableIfNeeded("franchises", "merchant");
        renameTableIfNeeded("user_franchise_assignments", "user_merchant_assignments");

        renameColumnIfNeeded("monthly_sales", "franchise_id", "merchant_id");
        renameColumnIfNeeded("user_merchant_assignments", "franchise_id", "merchant_id");
        renameColumnIfNeeded("ai_insight_histories", "franchise_id", "merchant_id");
        renameColumnIfNeeded("assignment_histories", "franchise_id", "merchant_id");

        normalizeMerchantIdPrefix();
    }

    private void renameTableIfNeeded(String oldName, String newName) {
        if (!tableExists(oldName) || tableExists(newName)) {
            return;
        }

        jdbcTemplate.execute("RENAME TABLE " + oldName + " TO " + newName);
    }

    private void renameColumnIfNeeded(String tableName, String oldName, String newName) {
        if (!tableExists(tableName) || !columnExists(tableName, oldName) || columnExists(tableName, newName)) {
            return;
        }

        executeIgnoringSchemaConflict("ALTER TABLE " + tableName + " RENAME COLUMN " + oldName + " TO " + newName);
    }

    private void normalizeMerchantIdPrefix() {
        if (!tableExists("merchant")) {
            return;
        }

        try {
            jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS=0");
            jdbcTemplate.update("UPDATE merchant SET id = CONCAT('M', SUBSTRING(id, 2)) WHERE id REGEXP '^F[0-9]+$'");
            updateMerchantIdPrefix("monthly_sales");
            updateMerchantIdPrefix("user_merchant_assignments");
            updateMerchantIdPrefix("ai_insight_histories");
            updateMerchantIdPrefix("assignment_histories");
        } finally {
            jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS=1");
        }
    }

    private void updateMerchantIdPrefix(String tableName) {
        if (!tableExists(tableName) || !columnExists(tableName, "merchant_id")) {
            return;
        }

        jdbcTemplate.update("UPDATE " + tableName + " SET merchant_id = CONCAT('M', SUBSTRING(merchant_id, 2)) WHERE merchant_id REGEXP '^F[0-9]+$'");
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*)
                        FROM information_schema.tables
                        WHERE table_schema = DATABASE()
                          AND table_name = ?
                        """,
                Integer.class,
                tableName);
        return count != null && count > 0;
    }

    private boolean columnExists(String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*)
                        FROM information_schema.columns
                        WHERE table_schema = DATABASE()
                          AND table_name = ?
                          AND column_name = ?
                        """,
                Integer.class,
                tableName,
                columnName);
        return count != null && count > 0;
    }

    private void executeIgnoringSchemaConflict(String sql) {
        try {
            jdbcTemplate.execute(sql);
        } catch (BadSqlGrammarException | DuplicateKeyException ex) {
            // The schema is already in the desired shape or this database version cannot apply the exact rename.
        }
    }

    private void ensureAssignmentHistorySchema() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS assignment_histories (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    merchant_id VARCHAR(20) NOT NULL,
                    previous_user_id VARCHAR(64),
                    new_user_id VARCHAR(64),
                    changed_by VARCHAR(64) NOT NULL,
                    change_reason VARCHAR(255),
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (id),
                    KEY idx_assignment_history_merchant (merchant_id, created_at),
                    KEY idx_assignment_history_user (new_user_id, created_at),
                    CONSTRAINT fk_assignment_history_merchant
                        FOREIGN KEY (merchant_id) REFERENCES merchant (id)
                        ON DELETE CASCADE,
                    CONSTRAINT fk_assignment_history_previous_user
                        FOREIGN KEY (previous_user_id) REFERENCES users (id)
                        ON DELETE SET NULL,
                    CONSTRAINT fk_assignment_history_new_user
                        FOREIGN KEY (new_user_id) REFERENCES users (id)
                        ON DELETE SET NULL,
                    CONSTRAINT fk_assignment_history_changed_by
                        FOREIGN KEY (changed_by) REFERENCES users (id)
                        ON DELETE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);
    }

    private void ensureMerchantLocationColumns() {
        addColumnIfMissing(
                "merchant",
                "location_status",
                "ALTER TABLE merchant ADD COLUMN location_status VARCHAR(20) NOT NULL DEFAULT 'UNVERIFIED' COMMENT '위치 좌표 검증 상태'");
        addColumnIfMissing(
                "merchant",
                "geocoded_at",
                "ALTER TABLE merchant ADD COLUMN geocoded_at TIMESTAMP NULL COMMENT '주소 기반 좌표 산출 시각'");
        addColumnIfMissing(
                "merchant",
                "geocode_source",
                "ALTER TABLE merchant ADD COLUMN geocode_source VARCHAR(50) COMMENT '좌표 산출 출처'");
        addColumnIfMissing(
                "merchant",
                "location_note",
                "ALTER TABLE merchant ADD COLUMN location_note VARCHAR(255) COMMENT '위치 검증 또는 보정 메모'");
        addColumnIfMissing(
                "merchant",
                "operational_status",
                "ALTER TABLE merchant ADD COLUMN operational_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '가맹점 관리 상태: ACTIVE, CLOSED, CONTRACT_ENDED, SUSPENDED'");
        addColumnIfMissing(
                "merchant",
                "closed_at",
                "ALTER TABLE merchant ADD COLUMN closed_at TIMESTAMP NULL COMMENT '관리 종료 처리 시각'");
        addColumnIfMissing(
                "merchant",
                "closure_note",
                "ALTER TABLE merchant ADD COLUMN closure_note VARCHAR(255) COMMENT '관리 종료 사유 또는 메모'");
        backfillExistingMerchantLocationStatus();
    }

    private void backfillExistingMerchantLocationStatus() {
        jdbcTemplate.update("""
                UPDATE merchant
                SET location_status = 'GEOCODED',
                    geocode_source = COALESCE(geocode_source, 'SEED_DATA'),
                    location_note = COALESCE(location_note, '기존 가맹점 좌표 데이터를 위치 상태로 보정했습니다.'),
                    geocoded_at = COALESCE(geocoded_at, CURRENT_TIMESTAMP)
                WHERE latitude IS NOT NULL
                  AND longitude IS NOT NULL
                  AND (location_status IS NULL OR location_status = 'UNVERIFIED')
                """);
    }

    private void addColumnIfMissing(String tableName, String columnName, String alterSql) {
        Integer count = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*)
                        FROM information_schema.columns
                        WHERE table_schema = DATABASE()
                          AND table_name = ?
                          AND column_name = ?
                        """,
                Integer.class,
                tableName,
                columnName);

        if (count == null || count == 0) {
            jdbcTemplate.execute(alterSql);
        }
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
    public List<Merchant> getMerchants(String userId, String role) {
        if (userId == null || userId.isBlank() || "ADMIN".equalsIgnoreCase(role)) {
            return loadAllMerchants();
        }

        List<String> assignedIds = loadAssignedMerchantIds(userId, role);
        if (assignedIds.isEmpty()) {
            return List.of();
        }

        return loadMerchantsByIds(assignedIds);
    }

    @Override
    public Map<String, Object> getAverages() {
        List<Merchant> merchants = loadAllMerchants();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("industryAverages", calculateAverages(merchants, Merchant::getIndustry));
        response.put("regionAverages", calculateAverages(merchants, Merchant::getRegion));
        return response;
    }

    @Override
    public List<AiInsightHistory> getAiInsights(String merchantId) {
        return jdbcTemplate.query("""
                        SELECT h.id, h.merchant_id, h.created_by, u.name AS created_by_name,
                               h.sales_month, h.risk_level, h.summary, h.content, h.note, h.tags, h.created_at
                        FROM ai_insight_histories h
                        JOIN users u ON u.id = h.created_by
                        WHERE h.merchant_id = ?
                        ORDER BY h.created_at DESC, h.id DESC
                        """,
                this::mapAiInsightHistory,
                merchantId);
    }

    @Override
    public AiInsightHistory getLatestAiInsight(String merchantId) {
        try {
            return jdbcTemplate.queryForObject("""
                            SELECT h.id, h.merchant_id, h.created_by, u.name AS created_by_name,
                                   h.sales_month, h.risk_level, h.summary, h.content, h.note, h.tags, h.created_at
                            FROM ai_insight_histories h
                            JOIN users u ON u.id = h.created_by
                            WHERE h.merchant_id = ?
                            ORDER BY h.created_at DESC, h.id DESC
                            LIMIT 1
                            """,
                    this::mapAiInsightHistory,
                    merchantId);
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }

    @Override
    public AiInsightHistory saveAiInsight(
            String merchantId,
            String createdBy,
            String salesMonth,
            String riskLevel,
            String summary,
            String content,
            String note,
            List<String> tags) {
        jdbcTemplate.update("""
                        INSERT INTO ai_insight_histories
                            (merchant_id, created_by, sales_month, risk_level, summary, content, note, tags)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                merchantId,
                createdBy,
                salesMonth,
                riskLevel,
                summary,
                content,
                note,
                String.join(",", tags));

        return getLatestAiInsight(merchantId);
    }

    @Override
    public AiInsightHistory updateAiInsightNote(Long insightId, String merchantId, String note) {
        int updated = jdbcTemplate.update("""
                        UPDATE ai_insight_histories
                        SET note = ?
                        WHERE id = ? AND merchant_id = ?
                        """,
                note,
                insightId,
                merchantId);

        if (updated == 0) {
            throw new IllegalArgumentException("존재하지 않는 AI 인사이트입니다.");
        }

        return jdbcTemplate.queryForObject("""
                        SELECT h.id, h.merchant_id, h.created_by, u.name AS created_by_name,
                               h.sales_month, h.risk_level, h.summary, h.content, h.note, h.tags, h.created_at
                        FROM ai_insight_histories h
                        JOIN users u ON u.id = h.created_by
                        WHERE h.id = ? AND h.merchant_id = ?
                        """,
                this::mapAiInsightHistory,
                insightId,
                merchantId);
    }

    @Override
    @Transactional
    public Merchant createMerchant(
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
            String changedBy) {
        if (managerId != null && !managerId.isBlank() && !existsSalesUser(managerId)) {
            throw new IllegalArgumentException("존재하지 않는 영업사원입니다.");
        }

        String merchantId = nextMerchantId();
        jdbcTemplate.update("""
                        INSERT INTO merchant
                            (id, name, industry, region, address, latitude, longitude,
                             location_status, geocoded_at, geocode_source, location_note,
                             operational_status)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, CASE WHEN ? IS NULL THEN NULL ELSE CURRENT_TIMESTAMP END, ?, ?, 'ACTIVE')
                        """,
                merchantId,
                name,
                industry,
                region,
                address,
                latitude,
                longitude,
                locationStatus,
                latitude,
                geocodeSource,
                locationNote);

        if (managerId != null && !managerId.isBlank()) {
            jdbcTemplate.update("""
                            INSERT INTO user_merchant_assignments (user_id, merchant_id)
                            VALUES (?, ?)
                            """,
                    managerId,
                    merchantId);
            recordAssignmentHistory(merchantId, null, managerId, changedBy, "신규 가맹점 등록 시 담당자를 배정했습니다.");
        }

        return loadMerchantById(merchantId);
    }

    @Override
    public Merchant updateMerchant(
            String merchantId,
            String name,
            String industry,
            String region,
            String address,
            Double latitude,
            Double longitude,
            String locationStatus,
            String geocodeSource,
            String locationNote) {
        int updated = jdbcTemplate.update("""
                        UPDATE merchant
                        SET name = ?,
                            industry = ?,
                            region = ?,
                            address = ?,
                            latitude = ?,
                            longitude = ?,
                            location_status = ?,
                            geocoded_at = CASE WHEN ? IS NULL THEN NULL ELSE CURRENT_TIMESTAMP END,
                            geocode_source = ?,
                            location_note = ?
                        WHERE id = ?
                          AND operational_status = 'ACTIVE'
                        """,
                name,
                industry,
                region,
                address,
                latitude,
                longitude,
                locationStatus,
                latitude,
                geocodeSource,
                locationNote,
                merchantId);

        if (updated == 0) {
            throw new IllegalArgumentException("존재하지 않거나 관리 종료 처리된 가맹점입니다.");
        }

        return loadMerchantById(merchantId);
    }

    @Override
    @Transactional
    public void updateMerchantStatus(String merchantId, String operationalStatus, String statusNote) {
        int updated = jdbcTemplate.update("""
                        UPDATE merchant
                        SET operational_status = ?,
                            closed_at = CURRENT_TIMESTAMP,
                            closure_note = ?
                        WHERE id = ?
                          AND operational_status = 'ACTIVE'
                        """,
                operationalStatus,
                statusNote,
                merchantId);

        if (updated == 0) {
            throw new IllegalArgumentException("존재하지 않거나 이미 관리 종료 처리된 가맹점입니다.");
        }

        jdbcTemplate.update(
                "DELETE FROM user_merchant_assignments WHERE merchant_id = ?",
                merchantId);
    }

    @Override
    @Transactional
    public void assignManager(String merchantId, String managerId, String changedBy, String changeReason) {
        if (!existsActiveMerchant(merchantId)) {
            throw new IllegalArgumentException("존재하지 않는 가맹점입니다.");
        }

        if (managerId != null && !managerId.isBlank() && !existsSalesUser(managerId)) {
            throw new IllegalArgumentException("존재하지 않는 영업사원입니다.");
        }

        String previousUserId = findAssignedManagerId(merchantId);
        String normalizedManagerId = managerId == null || managerId.isBlank() ? null : managerId;
        if ((previousUserId == null && normalizedManagerId == null)
                || (previousUserId != null && previousUserId.equals(normalizedManagerId))) {
            return;
        }

        jdbcTemplate.update(
                "DELETE FROM user_merchant_assignments WHERE merchant_id = ?",
                merchantId);

        if (normalizedManagerId != null) {
            jdbcTemplate.update("""
                            INSERT INTO user_merchant_assignments (user_id, merchant_id)
                            VALUES (?, ?)
                            """,
                    normalizedManagerId,
                    merchantId);
        }
        recordAssignmentHistory(merchantId, previousUserId, normalizedManagerId, changedBy, changeReason);
    }

    @Override
    public List<AssignmentHistory> getAssignmentHistories() {
        return jdbcTemplate.query("""
                        SELECT h.id, h.merchant_id, f.name AS merchant_name,
                               h.previous_user_id, previous_user.name AS previous_user_name,
                               h.new_user_id, new_user.name AS new_user_name,
                               h.changed_by, changed_by_user.name AS changed_by_name,
                               h.change_reason, h.created_at
                        FROM assignment_histories h
                        JOIN merchant f ON f.id = h.merchant_id
                        LEFT JOIN users previous_user ON previous_user.id = h.previous_user_id
                        LEFT JOIN users new_user ON new_user.id = h.new_user_id
                        JOIN users changed_by_user ON changed_by_user.id = h.changed_by
                        ORDER BY h.created_at DESC, h.id DESC
                        LIMIT 100
                        """,
                this::mapAssignmentHistory);
    }

    @Override
    public Merchant updateMerchantLocation(
            String merchantId,
            Double latitude,
            Double longitude,
            String locationStatus,
            String geocodeSource,
            String locationNote) {
        int updated = jdbcTemplate.update("""
                        UPDATE merchant
                        SET latitude = ?,
                            longitude = ?,
                            location_status = ?,
                            geocoded_at = CASE WHEN ? IS NULL THEN geocoded_at ELSE CURRENT_TIMESTAMP END,
                            geocode_source = ?,
                            location_note = ?
                        WHERE id = ?
                          AND operational_status = 'ACTIVE'
                        """,
                latitude,
                longitude,
                locationStatus,
                latitude,
                geocodeSource,
                locationNote,
                merchantId);

        if (updated == 0) {
            throw new IllegalArgumentException("존재하지 않거나 관리 종료 처리된 가맹점입니다.");
        }

        return loadMerchantById(merchantId);
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

    private List<Merchant> loadAllMerchants() {
        List<Merchant> merchants = jdbcTemplate.query("""
                SELECT id, name, industry, region, address, latitude, longitude,
                       location_status, geocoded_at, geocode_source, location_note,
                       operational_status, closed_at, closure_note
                FROM merchant
                WHERE operational_status = 'ACTIVE'
                ORDER BY id
                """, this::mapMerchant);
        return attachMonthlySales(merchants);
    }

    private Merchant loadMerchantById(String merchantId) {
        Merchant merchant = jdbcTemplate.queryForObject("""
                        SELECT id, name, industry, region, address, latitude, longitude,
                               location_status, geocoded_at, geocode_source, location_note,
                               operational_status, closed_at, closure_note
                        FROM merchant
                        WHERE id = ?
                        """,
                this::mapMerchant,
                merchantId);
        return attachMonthlySales(List.of(merchant)).get(0);
    }

    private List<Merchant> loadMerchantsByIds(List<String> merchantIds) {
        List<Merchant> merchants = namedJdbcTemplate.query("""
                        SELECT id, name, industry, region, address, latitude, longitude,
                               location_status, geocoded_at, geocode_source, location_note,
                               operational_status, closed_at, closure_note
                        FROM merchant
                        WHERE id IN (:ids)
                          AND operational_status = 'ACTIVE'
                        ORDER BY id
                        """,
                Map.of("ids", merchantIds),
                this::mapMerchant);
        return attachMonthlySales(merchants);
    }

    private List<Merchant> attachMonthlySales(List<Merchant> merchants) {
        if (merchants.isEmpty()) {
            return merchants;
        }

        Map<String, Merchant> merchantById = merchants.stream()
                .collect(Collectors.toMap(
                        Merchant::getId,
                        Function.identity(),
                        (left, right) -> left,
                        LinkedHashMap::new));

        namedJdbcTemplate.query("""
                        SELECT id, merchant_id, sales_month, sales, tx_count, avg_ticket
                        FROM monthly_sales
                        WHERE merchant_id IN (:ids)
                        ORDER BY merchant_id, sales_month
                        """,
                Map.of("ids", merchantById.keySet()),
                rs -> {
                    Merchant merchant = merchantById.get(rs.getString("merchant_id"));
                    if (merchant != null) {
                        merchant.getMonthlySales().add(mapMonthlySales(rs));
                    }
                });

        return merchants;
    }

    private List<String> loadAssignedMerchantIds(String userId, String role) {
        if (!"SALES".equalsIgnoreCase(role)) {
            return List.of();
        }

        return jdbcTemplate.queryForList("""
                SELECT merchant_id
                FROM user_merchant_assignments a
                JOIN merchant f ON f.id = a.merchant_id
                WHERE a.user_id = ?
                  AND f.operational_status = 'ACTIVE'
                ORDER BY a.merchant_id
                """, String.class, userId);
    }

    private boolean existsActiveMerchant(String id) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM merchant WHERE id = ? AND operational_status = 'ACTIVE'",
                Integer.class,
                id);
        return count != null && count > 0;
    }

    private String findAssignedManagerId(String merchantId) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT user_id FROM user_merchant_assignments WHERE merchant_id = ? LIMIT 1",
                    String.class,
                    merchantId);
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }

    private void recordAssignmentHistory(
            String merchantId,
            String previousUserId,
            String newUserId,
            String changedBy,
            String changeReason) {
        if (changedBy == null || changedBy.isBlank()) {
            return;
        }

        jdbcTemplate.update("""
                        INSERT INTO assignment_histories
                            (merchant_id, previous_user_id, new_user_id, changed_by, change_reason)
                        VALUES (?, ?, ?, ?, ?)
                        """,
                merchantId,
                previousUserId,
                newUserId,
                changedBy,
                changeReason);
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

    private String nextMerchantId() {
        List<String> ids = jdbcTemplate.queryForList("SELECT id FROM merchant", String.class);
        OptionalInt maxId = ids.stream()
                .filter(id -> id != null && id.matches("M\\d+"))
                .mapToInt(id -> Integer.parseInt(id.substring(1)))
                .max();
        return "M%03d".formatted(maxId.orElse(0) + 1);
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

    private Merchant mapMerchant(ResultSet rs, int rowNum) throws SQLException {
        Merchant merchant = new Merchant();
        merchant.setId(rs.getString("id"));
        merchant.setName(rs.getString("name"));
        merchant.setIndustry(rs.getString("industry"));
        merchant.setRegion(rs.getString("region"));
        merchant.setAddress(rs.getString("address"));
        merchant.setLatitude(nullableDouble(rs, "latitude"));
        merchant.setLongitude(nullableDouble(rs, "longitude"));
        merchant.setLocationStatus(rs.getString("location_status"));
        Timestamp geocodedAt = rs.getTimestamp("geocoded_at");
        merchant.setGeocodedAt(geocodedAt == null ? null : geocodedAt.toInstant());
        merchant.setGeocodeSource(rs.getString("geocode_source"));
        merchant.setLocationNote(rs.getString("location_note"));
        merchant.setOperationalStatus(rs.getString("operational_status"));
        Timestamp closedAt = rs.getTimestamp("closed_at");
        merchant.setClosedAt(closedAt == null ? null : closedAt.toInstant());
        merchant.setClosureNote(rs.getString("closure_note"));
        merchant.setMonthlySales(new ArrayList<>());
        return merchant;
    }

    private MonthlySales mapMonthlySales(ResultSet rs) throws SQLException {
        MonthlySales monthlySales = new MonthlySales();
        monthlySales.setId(rs.getLong("id"));
        monthlySales.setMerchantId(rs.getString("merchant_id"));
        monthlySales.setMonth(rs.getString("sales_month"));
        monthlySales.setSales(rs.getLong("sales"));
        monthlySales.setTxCount(rs.getInt("tx_count"));
        monthlySales.setAvgTicket(rs.getInt("avg_ticket"));
        return monthlySales;
    }

    private AiInsightHistory mapAiInsightHistory(ResultSet rs, int rowNum) throws SQLException {
        AiInsightHistory history = new AiInsightHistory();
        history.setId(rs.getLong("id"));
        history.setMerchantId(rs.getString("merchant_id"));
        history.setCreatedBy(rs.getString("created_by"));
        history.setCreatedByName(rs.getString("created_by_name"));
        history.setSalesMonth(rs.getString("sales_month"));
        history.setRiskLevel(rs.getString("risk_level"));
        history.setSummary(rs.getString("summary"));
        history.setContent(rs.getString("content"));
        history.setNote(rs.getString("note"));
        history.setTags(splitTags(rs.getString("tags")));

        Timestamp createdAt = rs.getTimestamp("created_at");
        history.setCreatedAt(createdAt == null ? null : createdAt.toInstant());
        return history;
    }

    private AssignmentHistory mapAssignmentHistory(ResultSet rs, int rowNum) throws SQLException {
        AssignmentHistory history = new AssignmentHistory();
        history.setId(rs.getLong("id"));
        history.setMerchantId(rs.getString("merchant_id"));
        history.setMerchantName(rs.getString("merchant_name"));
        history.setPreviousUserId(rs.getString("previous_user_id"));
        history.setPreviousUserName(rs.getString("previous_user_name"));
        history.setNewUserId(rs.getString("new_user_id"));
        history.setNewUserName(rs.getString("new_user_name"));
        history.setChangedBy(rs.getString("changed_by"));
        history.setChangedByName(rs.getString("changed_by_name"));
        history.setChangeReason(rs.getString("change_reason"));

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
        copy.setAssignedMerchantIds(loadAssignedMerchantIds(user.getId(), user.getRole()));
        copy.setPermissions(new HashMap<>(user.getPermissions()));
        return copy;
    }

    private Map<String, Map<String, Object>> calculateAverages(
            List<Merchant> merchants,
            Function<Merchant, String> groupBy) {
        Map<String, Map<String, List<Long>>> salesByGroupAndMonth = new LinkedHashMap<>();

        for (Merchant merchant : merchants) {
            String groupName = groupBy.apply(merchant);
            Map<String, List<Long>> salesByMonth = salesByGroupAndMonth.computeIfAbsent(
                    groupName,
                    key -> new LinkedHashMap<>());

            for (MonthlySales monthlySales : merchant.getMonthlySales()) {
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
