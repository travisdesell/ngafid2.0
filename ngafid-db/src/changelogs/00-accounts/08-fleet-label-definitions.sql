--liquibase formatted sql

--changeset roman:label-definitions-table labels:accounts,labels
CREATE TABLE label_definitions (
    id INT NOT NULL AUTO_INCREMENT,
    fleet_id INT NOT NULL,
    label_text VARCHAR(256) NOT NULL,
    display_order INT NOT NULL DEFAULT 0,

    PRIMARY KEY (id),
    UNIQUE KEY uk_label_definitions_fleet_text (fleet_id, label_text),
    INDEX idx_label_definitions_fleet_id (fleet_id),
    CONSTRAINT fk_label_definitions_fleet
        FOREIGN KEY (fleet_id) REFERENCES fleet(id)
            ON DELETE CASCADE
);

--changeset roman:label-definitions-seed labels:accounts,labels
-- Seed 39 predefined cluster labels for every existing fleet (numbered for order and dropdown search).
INSERT INTO label_definitions (fleet_id, label_text, display_order)
SELECT f.id, t.label_text, t.display_order
FROM fleet f
CROSS JOIN (
    SELECT 1 AS display_order, '1-aircraft start/external issue' AS label_text
    UNION ALL SELECT 2, '2-baffle bolt miss/damage'
    UNION ALL SELECT 3, '3-baffle bracket loose/damage'
    UNION ALL SELECT 4, '4-baffle crack/damage/loose/miss'
    UNION ALL SELECT 5, '5-baffle mount loose/damage'
    UNION ALL SELECT 6, '6-baffle plug need repair/replace'
    UNION ALL SELECT 7, '7-baffle rivet loose/miss/damage'
    UNION ALL SELECT 8, '8-baffle screw miss/loose'
    UNION ALL SELECT 9, '9-baffle seal loose/damage'
    UNION ALL SELECT 10, '10-baffle spring damage'
    UNION ALL SELECT 11, '11-baffle tie/tie rod loose or damage'
    UNION ALL SELECT 12, '12-cowling miss/loose/damage'
    UNION ALL SELECT 13, '13-cylinder compression issue'
    UNION ALL SELECT 14, '14-cylinder crack/fail/need part repair'
    UNION ALL SELECT 15, '15-cylinder exhaust valve/stuck valve issue'
    UNION ALL SELECT 16, '16-cylinder head/exhaust gas temperature issue'
    UNION ALL SELECT 17, '17-cylinder/exhaust push rod/tube damage'
    UNION ALL SELECT 18, '18-drain line/tube damage'
    UNION ALL SELECT 19, '19-engine carburetor need maintenance'
    UNION ALL SELECT 20, '20-engine crankcase/crankshaft/firewall near repair'
    UNION ALL SELECT 21, '21-engine failure/fire/time out'
    UNION ALL SELECT 22, '22-engine idle/rpm issue'
    UNION ALL SELECT 23, '23-engine need repair/reinstall/clean'
    UNION ALL SELECT 24, '24-engine run rough'
    UNION ALL SELECT 25, '25-engine seal/tube/bolt loose or damage'
    UNION ALL SELECT 26, '26-engine/propeller overspeed or damage'
    UNION ALL SELECT 27, '27-induction damage/hardware fail'
    UNION ALL SELECT 28, '28-intake gasket leak/damage'
    UNION ALL SELECT 29, '29-intake tube/bolt/seal/boot loose or damage'
    UNION ALL SELECT 30, '30-magneto failure'
    UNION ALL SELECT 31, '31-mixture fail/need adjust'
    UNION ALL SELECT 32, '32-oil cooler need maintenance'
    UNION ALL SELECT 33, '33-oil dipstick/tube need repair'
    UNION ALL SELECT 34, '34-oil leak/pressure issue'
    UNION ALL SELECT 35, '35-oil return line issue'
    UNION ALL SELECT 36, '36-pilot/in-flight noticed issue'
    UNION ALL SELECT 37, '37-rocker cover leak/loose/damage'
    UNION ALL SELECT 38, '38-sniffler valve need maintenance'
    UNION ALL SELECT 39, '39-spark plug need repair/replace'
) t;
