--liquibase formatted sql

--changeset pujan:logs labels:accounts
CREATE TABLE api_logs (
  id INT NOT NULL AUTO_INCREMENT,
  method VARCHAR(10),
  url TEXT,
  path VARCHAR(512),
  status_code VARCHAR(10),
  ip VARCHAR(45),
  referer TEXT,
  timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

  PRIMARY KEY (id)
);