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

CREATE TABLE IF NOT EXISTS franchises (
    id VARCHAR(20) NOT NULL COMMENT '가맹점 고유 ID',
    name VARCHAR(120) NOT NULL COMMENT '가맹점명',
    industry VARCHAR(60) NOT NULL COMMENT '가맹점 업종',
    region VARCHAR(100) NOT NULL COMMENT '가맹점이 속한 지역',
    address VARCHAR(255) NOT NULL COMMENT '가맹점 주소',
    latitude DECIMAL(10, 7) COMMENT '지도 표시용 위도',
    longitude DECIMAL(10, 7) COMMENT '지도 표시용 경도',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '가맹점 정보 생성 시각',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '가맹점 정보 최종 수정 시각',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='가맹점 기본 정보';

CREATE TABLE IF NOT EXISTS monthly_sales (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '월별 매출 데이터 고유 ID',
    franchise_id VARCHAR(20) NOT NULL COMMENT '매출 데이터가 속한 가맹점 ID',
    sales_month CHAR(7) NOT NULL COMMENT '매출 기준 월: YYYY-MM 형식',
    sales BIGINT NOT NULL COMMENT '해당 월 총 매출액',
    tx_count INT NOT NULL COMMENT '해당 월 결제 건수',
    avg_ticket INT NOT NULL COMMENT '해당 월 평균 객단가',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '월별 매출 데이터 생성 시각',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '월별 매출 데이터 최종 수정 시각',
    PRIMARY KEY (id),
    UNIQUE KEY uk_monthly_sales_franchise_month (franchise_id, sales_month),
    CONSTRAINT fk_monthly_sales_franchise
        FOREIGN KEY (franchise_id) REFERENCES franchises (id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='가맹점별 월간 매출 지표';

CREATE TABLE IF NOT EXISTS user_franchise_assignments (
    user_id VARCHAR(64) NOT NULL COMMENT '담당 사용자 ID',
    franchise_id VARCHAR(20) NOT NULL COMMENT '담당 가맹점 ID',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '담당 배정 생성 시각',
    PRIMARY KEY (user_id, franchise_id),
    CONSTRAINT fk_assignment_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_assignment_franchise
        FOREIGN KEY (franchise_id) REFERENCES franchises (id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='영업 담당자와 가맹점 배정 관계';

CREATE TABLE IF NOT EXISTS ai_insight_histories (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'AI 분석 이력 고유 ID',
    franchise_id VARCHAR(20) NOT NULL COMMENT 'AI 분석 대상 가맹점 ID',
    created_by VARCHAR(64) NOT NULL COMMENT 'AI 분석을 생성한 사용자 ID',
    sales_month CHAR(7) NOT NULL COMMENT 'AI 분석 기준 매출 월: YYYY-MM 형식',
    risk_level VARCHAR(20) NOT NULL COMMENT '분석 생성 시점의 가맹점 위험 등급',
    summary VARCHAR(500) NOT NULL COMMENT 'AI 분석 결과 요약',
    content TEXT NOT NULL COMMENT 'AI가 생성한 전체 분석 내용',
    note TEXT COMMENT '담당자가 남긴 후속 조치 또는 확인 메모',
    tags VARCHAR(255) COMMENT '분석 결과와 연결된 운영 태그 목록',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'AI 분석 이력 생성 시각',
    PRIMARY KEY (id),
    KEY idx_ai_insight_franchise_month (franchise_id, sales_month),
    CONSTRAINT fk_ai_insight_franchise
        FOREIGN KEY (franchise_id) REFERENCES franchises (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_ai_insight_user
        FOREIGN KEY (created_by) REFERENCES users (id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='가맹점별 AI 운영 인사이트 생성 이력';
