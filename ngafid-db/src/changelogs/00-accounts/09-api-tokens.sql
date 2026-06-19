--liquibase formatted sql
--changeset darren:api-tokens labels:accounts
CREATE TABLE api_token (
    id INT NOT NULL AUTO_INCREMENT,
    user_id INT NOT NULL,

    token_hash CHAR(64) NOT NULL COMMENT 'SHA-256 hex of plaintext token',
    token_name VARCHAR(128) NOT NULL,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at DATETIME DEFAULT NULL,
    revoked_at DATETIME DEFAULT NULL,
    last_used_at DATETIME DEFAULT NULL,

    PRIMARY KEY (id),
    UNIQUE KEY (token_hash),
    KEY idx_api_token_user (user_id),
    FOREIGN KEY (user_id) REFERENCES user(id)
        ON DELETE CASCADE
);