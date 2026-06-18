-- Existing database comment migration.
-- Run this once against an already-created MySQL database when schema.sql
-- comments need to be reflected without recreating tables.

ALTER TABLE users COMMENT = '서비스 사용자 계정과 권한 정보';
ALTER TABLE users
    MODIFY id VARCHAR(64) NOT NULL COMMENT '사용자 로그인 ID',
    MODIFY password_hash VARCHAR(255) NOT NULL COMMENT 'PBKDF2로 해시한 사용자 비밀번호',
    MODIFY name VARCHAR(100) NOT NULL COMMENT '사용자 표시 이름',
    MODIFY role VARCHAR(20) NOT NULL COMMENT '사용자 역할: ADMIN 또는 SALES',
    MODIFY can_use_ai BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'AI 분석 기능 사용 가능 여부',
    MODIFY created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '사용자 계정 생성 시각',
    MODIFY updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '사용자 계정 최종 수정 시각';

ALTER TABLE merchant COMMENT = '가맹점 기본 정보';
ALTER TABLE merchant
    MODIFY id VARCHAR(20) NOT NULL COMMENT '가맹점 고유 ID',
    MODIFY name VARCHAR(120) NOT NULL COMMENT '가맹점명',
    MODIFY industry VARCHAR(60) NOT NULL COMMENT '가맹점 업종',
    MODIFY region VARCHAR(100) NOT NULL COMMENT '가맹점이 속한 지역',
    MODIFY address VARCHAR(255) NOT NULL COMMENT '가맹점 주소',
    MODIFY latitude DECIMAL(10, 7) COMMENT '지도 표시용 위도',
    MODIFY longitude DECIMAL(10, 7) COMMENT '지도 표시용 경도',
    MODIFY location_status VARCHAR(20) NOT NULL DEFAULT 'UNVERIFIED' COMMENT '위치 좌표 검증 상태',
    MODIFY geocoded_at TIMESTAMP NULL COMMENT '주소 기반 좌표 산출 시각',
    MODIFY geocode_source VARCHAR(50) COMMENT '좌표 산출 출처',
    MODIFY location_note VARCHAR(255) COMMENT '위치 검증 또는 보정 메모',
    MODIFY operational_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '가맹점 관리 상태: ACTIVE, CLOSED, CONTRACT_ENDED, SUSPENDED',
    MODIFY closed_at TIMESTAMP NULL COMMENT '관리 종료 처리 시각',
    MODIFY closure_note VARCHAR(255) COMMENT '관리 종료 사유 또는 메모',
    MODIFY created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '가맹점 정보 생성 시각',
    MODIFY updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '가맹점 정보 최종 수정 시각';

ALTER TABLE monthly_sales COMMENT = '가맹점별 월간 매출 지표';
ALTER TABLE monthly_sales
    MODIFY id BIGINT NOT NULL AUTO_INCREMENT COMMENT '월별 매출 데이터 고유 ID',
    MODIFY merchant_id VARCHAR(20) NOT NULL COMMENT '매출 데이터가 속한 가맹점 ID',
    MODIFY sales_month CHAR(7) NOT NULL COMMENT '매출 기준 월: YYYY-MM 형식',
    MODIFY sales BIGINT NOT NULL COMMENT '해당 월 총 매출액',
    MODIFY tx_count INT NOT NULL COMMENT '해당 월 결제 건수',
    MODIFY avg_ticket INT NOT NULL COMMENT '해당 월 평균 객단가',
    MODIFY created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '월별 매출 데이터 생성 시각',
    MODIFY updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '월별 매출 데이터 최종 수정 시각';

ALTER TABLE user_merchant_assignments COMMENT = '영업 담당자와 가맹점 배정 관계';
ALTER TABLE user_merchant_assignments
    MODIFY user_id VARCHAR(64) NOT NULL COMMENT '담당 사용자 ID',
    MODIFY merchant_id VARCHAR(20) NOT NULL COMMENT '담당 가맹점 ID',
    MODIFY created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '담당 배정 생성 시각';

ALTER TABLE ai_insight_histories COMMENT = '가맹점별 AI 운영 인사이트 생성 이력';
ALTER TABLE ai_insight_histories
    MODIFY id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'AI 분석 이력 고유 ID',
    MODIFY merchant_id VARCHAR(20) NOT NULL COMMENT 'AI 분석 대상 가맹점 ID',
    MODIFY created_by VARCHAR(64) NOT NULL COMMENT 'AI 분석을 생성한 사용자 ID',
    MODIFY sales_month CHAR(7) NOT NULL COMMENT 'AI 분석 기준 매출 월: YYYY-MM 형식',
    MODIFY risk_level VARCHAR(20) NOT NULL COMMENT '분석 생성 시점의 가맹점 위험 등급',
    MODIFY summary VARCHAR(500) NOT NULL COMMENT 'AI 분석 결과 요약',
    MODIFY content TEXT NOT NULL COMMENT 'AI가 생성한 전체 분석 내용',
    MODIFY note TEXT COMMENT '담당자가 남긴 후속 조치 또는 확인 메모',
    MODIFY tags VARCHAR(255) COMMENT '분석 결과와 연결된 운영 태그 목록',
    MODIFY created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'AI 분석 이력 생성 시각';
