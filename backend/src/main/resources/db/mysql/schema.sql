CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(64) NOT NULL COMMENT '사용자 로그인 ID',
    password_hash VARCHAR(255) NOT NULL COMMENT 'PBKDF2로 해시한 사용자 비밀번호',
    name VARCHAR(100) NOT NULL COMMENT '사용자 표시 이름',
    role VARCHAR(20) NOT NULL COMMENT '사용자 역할: ADMIN 또는 SALES',
    can_use_ai BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'AI 분석 기능 사용 가능 여부',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '사용자 계정 생성 시각',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '사용자 계정 최종 수정 시각',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='서비스 사용자 계정과 권한 정보';

CREATE TABLE IF NOT EXISTS merchant (
    id VARCHAR(20) NOT NULL COMMENT '가맹점 고유 ID',
    name VARCHAR(120) NOT NULL COMMENT '가맹점명',
    industry VARCHAR(60) NOT NULL COMMENT '가맹점 업종',
    region VARCHAR(100) NOT NULL COMMENT '가맹점이 속한 지역',
    address VARCHAR(255) NOT NULL COMMENT '가맹점 주소',
    latitude DECIMAL(10, 7) COMMENT '지도 표시용 위도',
    longitude DECIMAL(10, 7) COMMENT '지도 표시용 경도',
    location_status VARCHAR(20) NOT NULL DEFAULT 'UNVERIFIED' COMMENT '위치 좌표 검증 상태',
    geocoded_at TIMESTAMP NULL COMMENT '주소 기반 좌표 산출 시각',
    geocode_source VARCHAR(50) COMMENT '좌표 산출 출처',
    location_note VARCHAR(255) COMMENT '위치 검증 또는 보정 메모',
    operational_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '가맹점 관리 상태: ACTIVE, CLOSED, CONTRACT_ENDED, SUSPENDED',
    closed_at TIMESTAMP NULL COMMENT '관리 종료 처리 시각',
    closure_note VARCHAR(255) COMMENT '관리 종료 사유 또는 메모',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '가맹점 정보 생성 시각',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '가맹점 정보 최종 수정 시각',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='가맹점 기본 정보';

CREATE TABLE IF NOT EXISTS monthly_sales (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '월별 매출 데이터 고유 ID',
    merchant_id VARCHAR(20) NOT NULL COMMENT '매출 데이터가 속한 가맹점 ID',
    sales_month CHAR(7) NOT NULL COMMENT '매출 기준 월: YYYY-MM 형식',
    sales BIGINT NOT NULL COMMENT '해당 월 총 매출액',
    tx_count INT NOT NULL COMMENT '해당 월 결제 건수',
    avg_ticket INT NOT NULL COMMENT '해당 월 평균 객단가',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '월별 매출 데이터 생성 시각',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '월별 매출 데이터 최종 수정 시각',
    PRIMARY KEY (id),
    UNIQUE KEY uk_monthly_sales_merchant_month (merchant_id, sales_month),
    CONSTRAINT fk_monthly_sales_merchant
        FOREIGN KEY (merchant_id) REFERENCES merchant (id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='가맹점별 월간 매출 지표';

CREATE TABLE IF NOT EXISTS user_merchant_assignments (
    user_id VARCHAR(64) NOT NULL COMMENT '담당 사용자 ID',
    merchant_id VARCHAR(20) NOT NULL COMMENT '담당 가맹점 ID',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '담당 배정 생성 시각',
    PRIMARY KEY (user_id, merchant_id),
    CONSTRAINT fk_assignment_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_assignment_merchant
        FOREIGN KEY (merchant_id) REFERENCES merchant (id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='영업 담당자와 가맹점 배정 관계';

CREATE TABLE IF NOT EXISTS assignment_histories (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '담당자 배정 이력 ID',
    merchant_id VARCHAR(20) NOT NULL COMMENT '가맹점 ID',
    previous_user_id VARCHAR(64) COMMENT '변경 전 담당 사용자 ID',
    new_user_id VARCHAR(64) COMMENT '변경 후 담당 사용자 ID',
    changed_by VARCHAR(64) NOT NULL COMMENT '변경을 수행한 관리자 ID',
    change_reason VARCHAR(255) COMMENT '담당자 변경 사유',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '담당자 변경 시각',
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='담당자 배정 변경 이력';

CREATE TABLE IF NOT EXISTS ai_insight_histories (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'AI 분석 이력 고유 ID',
    merchant_id VARCHAR(20) NOT NULL COMMENT 'AI 분석 대상 가맹점 ID',
    created_by VARCHAR(64) NOT NULL COMMENT 'AI 분석을 생성한 사용자 ID',
    sales_month CHAR(7) NOT NULL COMMENT 'AI 분석 기준 매출 월: YYYY-MM 형식',
    risk_level VARCHAR(20) NOT NULL COMMENT '분석 생성 시점의 가맹점 위험 등급',
    summary VARCHAR(500) NOT NULL COMMENT 'AI 분석 결과 요약',
    content TEXT NOT NULL COMMENT 'AI가 생성한 전체 분석 내용',
    note TEXT COMMENT '담당자가 남긴 후속 조치 또는 확인 메모',
    tags VARCHAR(255) COMMENT '분석 결과와 연결된 운영 태그 목록',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'AI 분석 이력 생성 시각',
    PRIMARY KEY (id),
    KEY idx_ai_insight_merchant_month (merchant_id, sales_month),
    CONSTRAINT fk_ai_insight_merchant
        FOREIGN KEY (merchant_id) REFERENCES merchant (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_ai_insight_user
        FOREIGN KEY (created_by) REFERENCES users (id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='가맹점별 AI 운영 인사이트 생성 이력';
CREATE TABLE IF NOT EXISTS sales_upload_histories (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Sales upload history ID',
    file_name VARCHAR(255) NOT NULL COMMENT 'Uploaded CSV file name',
    uploaded_by VARCHAR(64) NOT NULL COMMENT 'Admin user ID that committed the upload',
    total_rows INT NOT NULL DEFAULT 0 COMMENT 'Total parsed CSV rows',
    applied_rows INT NOT NULL DEFAULT 0 COMMENT 'Rows applied to monthly_sales',
    warning_rows INT NOT NULL DEFAULT 0 COMMENT 'Rows applied with warnings',
    status VARCHAR(30) NOT NULL COMMENT 'Upload status',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Upload committed time',
    PRIMARY KEY (id),
    KEY idx_sales_upload_created_at (created_at),
    CONSTRAINT fk_sales_upload_user
        FOREIGN KEY (uploaded_by) REFERENCES users (id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Sales CSV upload commit history';
