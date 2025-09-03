--liquibase formatted sql

--changeset josh:two-factor-auth labels:accounts,security
ALTER TABLE user ADD COLUMN two_factor_enabled BOOLEAN DEFAULT FALSE;
ALTER TABLE user ADD COLUMN two_factor_secret VARCHAR(32);
ALTER TABLE user ADD COLUMN backup_codes TEXT; -- JSON array of hashed backup codes
ALTER TABLE user ADD COLUMN two_factor_setup_complete BOOLEAN DEFAULT FALSE;
