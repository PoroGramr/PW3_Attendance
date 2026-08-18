ALTER TABLE admin_account
    ADD COLUMN email VARCHAR(254) NULL AFTER name;

UPDATE admin_account
SET email = CONCAT(username, '@legacy.invalid')
WHERE email IS NULL;

ALTER TABLE admin_account
    MODIFY COLUMN email VARCHAR(254) NOT NULL,
    ADD CONSTRAINT uk_admin_account_email UNIQUE (email);
