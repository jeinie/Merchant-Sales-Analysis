INSERT INTO users (id, password_hash, name, role, can_use_ai) VALUES
    ('admin', 'pbkdf2_sha256$120000$ZnJhbmNoaXNlLXNlZWQtMDE=$vsvg4uUdvd/QAiaT2ozwcSl90WiULN5JXr6qV0pmIH0=', '시스템 관리자', 'ADMIN', TRUE),
    ('sales_user', 'pbkdf2_sha256$120000$ZnJhbmNoaXNlLXNlZWQtMDE=$vsvg4uUdvd/QAiaT2ozwcSl90WiULN5JXr6qV0pmIH0=', '김영업 사원', 'SALES', TRUE),
    ('sales_user2', 'pbkdf2_sha256$120000$ZnJhbmNoaXNlLXNlZWQtMDE=$vsvg4uUdvd/QAiaT2ozwcSl90WiULN5JXr6qV0pmIH0=', '이영업 사원', 'SALES', TRUE)
ON DUPLICATE KEY UPDATE
    password_hash = VALUES(password_hash),
    name = VALUES(name),
    role = VALUES(role),
    can_use_ai = VALUES(can_use_ai);

INSERT INTO franchises (id, name, industry, region, address, latitude, longitude, location_status, geocode_source, location_note, operational_status) VALUES
    ('F001', '강남역 1호점', '카페', '서울 강남구', '서울 강남구 강남대로 396', 37.4979000, 127.0276000, 'GEOCODED', 'SEED_DATA', '초기 테스트 데이터 좌표', 'ACTIVE'),
    ('F002', '홍대입구점', '음식점', '서울 마포구', '서울 마포구 양화로 160', 37.5572000, 126.9245000, 'GEOCODED', 'SEED_DATA', '초기 테스트 데이터 좌표', 'ACTIVE'),
    ('F003', '여의도 금융타운점', '카페', '서울 영등포구', '서울 영등포구 여의대로 108', 37.5259000, 126.9286000, 'GEOCODED', 'SEED_DATA', '초기 테스트 데이터 좌표', 'ACTIVE'),
    ('F004', '성수 카페거리점', '카페', '서울 성동구', '서울 성동구 연무장길 14', 37.5438000, 127.0565000, 'GEOCODED', 'SEED_DATA', '초기 테스트 데이터 좌표', 'ACTIVE'),
    ('F005', '테헤란 점심특화점', '음식점', '서울 강남구', '서울 강남구 테헤란로 152', 37.5013000, 127.0396000, 'GEOCODED', 'SEED_DATA', '초기 테스트 데이터 좌표', 'ACTIVE'),
    ('F006', '합정 브런치점', '음식점', '서울 마포구', '서울 마포구 독막로 10', 37.5496000, 126.9139000, 'GEOCODED', 'SEED_DATA', '초기 테스트 데이터 좌표', 'ACTIVE'),
    ('F007', '샛강 테이크아웃점', '카페', '서울 영등포구', '서울 영등포구 의사당대로 83', 37.5184000, 126.9317000, 'GEOCODED', 'SEED_DATA', '초기 테스트 데이터 좌표', 'ACTIVE'),
    ('F008', '성수 로스터리점', '카페', '서울 성동구', '서울 성동구 성수이로 87', 37.5446000, 127.0557000, 'GEOCODED', 'SEED_DATA', '초기 테스트 데이터 좌표', 'ACTIVE'),
    ('F009', '상암 오피스푸드점', '음식점', '서울 마포구', '서울 마포구 월드컵북로 396', 37.5796000, 126.8896000, 'GEOCODED', 'SEED_DATA', '초기 테스트 데이터 좌표', 'ACTIVE'),
    ('F010', '선릉 라운지카페', '카페', '서울 강남구', '서울 강남구 선릉로 428', 37.5045000, 127.0490000, 'GEOCODED', 'SEED_DATA', '초기 테스트 데이터 좌표', 'ACTIVE')
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    industry = VALUES(industry),
    region = VALUES(region),
    address = VALUES(address),
    latitude = VALUES(latitude),
    longitude = VALUES(longitude),
    location_status = VALUES(location_status),
    geocode_source = VALUES(geocode_source),
    location_note = VALUES(location_note),
    operational_status = VALUES(operational_status),
    closed_at = NULL,
    closure_note = NULL;

INSERT INTO monthly_sales (id, franchise_id, sales_month, sales, tx_count, avg_ticket) VALUES
    (1, 'F001', '2023-01', 15000000, 1500, 10000),
    (2, 'F001', '2023-02', 16500000, 1600, 10312),
    (3, 'F001', '2023-03', 18000000, 1800, 10000),
    (4, 'F002', '2023-01', 25000000, 800, 31250),
    (5, 'F002', '2023-02', 23000000, 750, 30666),
    (6, 'F002', '2023-03', 28000000, 900, 31111),
    (7, 'F003', '2023-01', 22000000, 3000, 7333),
    (8, 'F003', '2023-02', 20000000, 2800, 7142),
    (9, 'F003', '2023-03', 25000000, 3200, 7812),
    (10, 'F004', '2023-01', 18000000, 1200, 15000),
    (11, 'F004', '2023-02', 20000000, 1300, 15384),
    (12, 'F004', '2023-03', 20400000, 1320, 15454),
    (13, 'F005', '2023-01', 17000000, 850, 20000),
    (14, 'F005', '2023-02', 19000000, 930, 20430),
    (15, 'F005', '2023-03', 16000000, 780, 20512),
    (16, 'F006', '2023-01', 26000000, 1040, 25000),
    (17, 'F006', '2023-02', 26500000, 1060, 25000),
    (18, 'F006', '2023-03', 27000000, 1080, 25000),
    (19, 'F007', '2023-01', 19000000, 2600, 7307),
    (20, 'F007', '2023-02', 18000000, 2500, 7200),
    (21, 'F007', '2023-03', 15000000, 2100, 7142),
    (22, 'F008', '2023-01', 14000000, 950, 14736),
    (23, 'F008', '2023-02', 15500000, 1050, 14761),
    (24, 'F008', '2023-03', 19000000, 1250, 15200),
    (25, 'F009', '2023-01', 30000000, 1200, 25000),
    (26, 'F009', '2023-02', 30600000, 1220, 25081),
    (27, 'F009', '2023-03', 31200000, 1240, 25161),
    (28, 'F010', '2023-01', 28000000, 1900, 14736),
    (29, 'F010', '2023-02', 27000000, 1850, 14594),
    (30, 'F010', '2023-03', 22000000, 1500, 14666)
ON DUPLICATE KEY UPDATE
    franchise_id = VALUES(franchise_id),
    sales_month = VALUES(sales_month),
    sales = VALUES(sales),
    tx_count = VALUES(tx_count),
    avg_ticket = VALUES(avg_ticket);

INSERT IGNORE INTO user_franchise_assignments (user_id, franchise_id) VALUES
    ('sales_user', 'F001'),
    ('sales_user', 'F002'),
    ('sales_user', 'F003'),
    ('sales_user', 'F004'),
    ('sales_user', 'F005'),
    ('sales_user2', 'F006'),
    ('sales_user2', 'F007'),
    ('sales_user2', 'F008'),
    ('sales_user2', 'F009'),
    ('sales_user2', 'F010');
