--liquibase formatted sql

--changeset josh:make-dummy-fleet labels:accounts,fleet
INSERT INTO fleet
VALUES (0, 'Test Fleet with ID 0'),
       (1, 'Test Fleet with ID 1');

--changeset josh:make-dummy-users labels:accounts,users
INSERT INTO user
VALUES (0, 'test@email.com', 'aaaaaaaaaaaaaaaaaaaa', 'John', 'Doe', '123 House Road', 'CityName', 'CountryName',
        'StateName', '10001', '', '', CURRENT_DATE, 0, 0, CURRENT_DATE),
       (1, 'test1@email.com', 'aaaaaaaaaaaaaaaaaaaa', 'John Admin', 'Aggregate Doe', '123 House Road', 'CityName',
        'CountryName', 'StateName', '10001', '', '', CURRENT_DATE, 1, 1, CURRENT_DATE);


--changeset josh:make-dummy-fleet-access labels:accounts,fleet
INSERT INTO fleet_access
VALUES (0, 0, 'VIEW'),
       (1, 0, 'MANAGER');

--changeset josh:make-dummy-airsync-info labels:accounts,fleet
INSERT INTO airsync_fleet_info
VALUES (1, 'Airsync Name', 'API_KEY', 'API_SECRET', current_date, NULL, NULL);