CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(64) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    name VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL,
    can_use_ai BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS franchises (
    id VARCHAR(20) NOT NULL,
    name VARCHAR(120) NOT NULL,
    industry VARCHAR(60) NOT NULL,
    region VARCHAR(100) NOT NULL,
    address VARCHAR(255) NOT NULL,
    latitude DECIMAL(10, 7),
    longitude DECIMAL(10, 7),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS monthly_sales (
    id BIGINT NOT NULL AUTO_INCREMENT,
    franchise_id VARCHAR(20) NOT NULL,
    sales_month CHAR(7) NOT NULL,
    sales BIGINT NOT NULL,
    tx_count INT NOT NULL,
    avg_ticket INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_monthly_sales_franchise_month (franchise_id, sales_month),
    CONSTRAINT fk_monthly_sales_franchise
        FOREIGN KEY (franchise_id) REFERENCES franchises (id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_franchise_assignments (
    user_id VARCHAR(64) NOT NULL,
    franchise_id VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, franchise_id),
    CONSTRAINT fk_assignment_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_assignment_franchise
        FOREIGN KEY (franchise_id) REFERENCES franchises (id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ai_insight_histories (
    id BIGINT NOT NULL AUTO_INCREMENT,
    franchise_id VARCHAR(20) NOT NULL,
    created_by VARCHAR(64) NOT NULL,
    sales_month CHAR(7) NOT NULL,
    risk_level VARCHAR(20) NOT NULL,
    summary VARCHAR(500) NOT NULL,
    content TEXT NOT NULL,
    tags VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_ai_insight_franchise_month (franchise_id, sales_month),
    CONSTRAINT fk_ai_insight_franchise
        FOREIGN KEY (franchise_id) REFERENCES franchises (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_ai_insight_user
        FOREIGN KEY (created_by) REFERENCES users (id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
