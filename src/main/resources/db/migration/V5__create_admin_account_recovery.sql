CREATE TABLE admin_account_recovery (
    id BIGINT NOT NULL AUTO_INCREMENT,
    public_id VARCHAR(36) NOT NULL,
    admin_id BIGINT NOT NULL,
    purpose VARCHAR(30) NOT NULL,
    channel VARCHAR(10) NOT NULL,
    code_hash VARCHAR(255) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    consumed_at DATETIME(6) NULL,
    failed_attempts INT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_admin_account_recovery_public_id UNIQUE (public_id),
    CONSTRAINT fk_admin_account_recovery_admin FOREIGN KEY (admin_id)
        REFERENCES admin_account (id) ON DELETE CASCADE,
    INDEX idx_admin_recovery_admin_purpose_created (admin_id, purpose, created_at),
    INDEX idx_admin_recovery_expires_at (expires_at)
) ENGINE=InnoDB;
