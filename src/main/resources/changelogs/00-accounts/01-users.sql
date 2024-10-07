--liquibase formatted sql

--changeset josh:user labels:accounts,users
CREATE TABLE user (
    id INT NOT NULL AUTO_INCREMENT,
    email VARCHAR(128) NOT NULL,
    password_token VARCHAR(64) NOT NULL,
    first_name VARCHAR(64),
    last_name VARCHAR(64),
    address VARCHAR(256) NOT NULL,
    city VARCHAR(64),
    country VARCHAR(128),
    state VARCHAR(64),
    zip_code VARCHAR(16),
    phone_number VARCHAR(24),
    reset_phrase VARCHAR(64),
    registration_time DATETIME,
    admin BOOLEAN DEFAULT 0,
    aggregate_view BOOLEAN DEFAULT 0,
    last_login_time DATETIME,

    PRIMARY KEY(id)
);

--changeset josh:user-preferences labels:accounts,users
CREATE TABLE user_preferences (
    user_id INT NOT NULL,
    decimal_precision INT NOT NULL,

    PRIMARY KEY(user_id),
    FOREIGN KEY(user_id) REFERENCES user(id)
);

--changeset josh:email-preferences labels:accounts,users,email
CREATE TABLE email_preferences (
    user_id INT NOT NULL,
    email_type VARCHAR(64) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT 1,

    PRIMARY KEY (user_id, email_type),
    FOREIGN KEY (user_id) REFERENCES user(id)
);

CREATE TABLE user_preferences_metrics (
    user_id INT NOT NULL,
    metric_id INT NOT NULL,

    PRIMARY KEY(user_id,metric_id),
    FOREIGN KEY(user_id) REFERENCES user(id)
);

CREATE TABLE email_unsubscribe_tokens (
    token VARCHAR(64) NOT NULL,
    user_id INT NOT NULL,
    expiration_date DATETIME NOT NULL,

    PRIMARY KEY (token),
    FOREIGN KEY (user_id) REFERENCES user(id)
);
