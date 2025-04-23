--liquibase formatted sql

--changeset pujan:logs labels:accounts
CREATE TABLE api_logs (
  id INT NOT NULL AUTO_INCREMENT,
  method VARCHAR(10),
  path VARCHAR(2048),
  status_code INT,
  ip VARBINARY(16),  -- supports both IPv4 and IPv6
  referer TEXT,
  timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);