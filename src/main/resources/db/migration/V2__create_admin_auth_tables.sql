CREATE TABLE admin_account (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    name VARCHAR(50) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    role VARCHAR(20) NOT NULL,
    approval_status VARCHAR(20) NOT NULL,
    approved_by BIGINT NULL,
    approved_at DATETIME(6) NULL,
    rejected_reason VARCHAR(500) NULL,
    last_login_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_admin_account_username UNIQUE (username),
    CONSTRAINT chk_admin_account_role CHECK (role IN ('ADMIN', 'SUPER_ADMIN')),
    CONSTRAINT chk_admin_account_approval_status CHECK (approval_status IN ('PENDING', 'APPROVED', 'REJECTED')),
    CONSTRAINT fk_admin_account_approved_by FOREIGN KEY (approved_by)
        REFERENCES admin_account (id) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE admin_refresh_token (
    id BIGINT NOT NULL AUTO_INCREMENT,
    admin_id BIGINT NOT NULL,
    token_hash CHAR(64) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    revoked_at DATETIME(6) NULL,
    device_info VARCHAR(255) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_admin_refresh_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_admin_refresh_token_admin FOREIGN KEY (admin_id)
        REFERENCES admin_account (id) ON DELETE CASCADE,
    INDEX idx_admin_refresh_token_admin_revoked (admin_id, revoked_at),
    INDEX idx_admin_refresh_token_expires_at (expires_at)
) ENGINE=InnoDB;
