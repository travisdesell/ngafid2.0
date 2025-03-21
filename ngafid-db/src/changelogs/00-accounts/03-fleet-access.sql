--liquibase formatted sql

--changeset josh:fleet-access labels:accounts
CREATE TABLE fleet_access (
    user_id INT,
    fleet_id INT,
    type VARCHAR(32),

    PRIMARY KEY (user_id, fleet_id),
    FOREIGN KEY (user_id) REFERENCES user(id),
    FOREIGN KEY (fleet_id) REFERENCES fleet(id)
);
