-- MySQL dump 10.13  Distrib 8.0.29, for Linux (x86_64)
--
-- Host: localhost    Database: ngafid
-- ------------------------------------------------------
-- Server version	8.0.29-0ubuntu0.22.04.2

/*!40101 SET @OLD_CHARACTER_SET_CLIENT = @@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS = @@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION = @@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE = @@TIME_ZONE */;
/*!40103 SET TIME_ZONE = '+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS = @@UNIQUE_CHECKS, UNIQUE_CHECKS = 0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS = @@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS = 0 */;
/*!40101 SET @OLD_SQL_MODE = @@SQL_MODE, SQL_MODE = 'NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES = @@SQL_NOTES, SQL_NOTES = 0 */;

--
-- Table structure for table `event_definitions`
--

DROP TABLE IF EXISTS `event_definitions`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `event_definitions`
(
    `id`                    int         NOT NULL AUTO_INCREMENT,
    `fleet_id`              int         NOT NULL,
    `airframe_id`           int         NOT NULL,
    `name`                  varchar(64) NOT NULL,
    `start_buffer`          int          DEFAULT NULL,
    `stop_buffer`           int          DEFAULT NULL,
    `column_names`          varchar(128) DEFAULT NULL,
    `condition_json`        varchar(512) DEFAULT NULL,
    `severity_column_names` varchar(128) DEFAULT NULL,
    `severity_type`         varchar(7)   DEFAULT NULL,
    `color`                 varchar(6)   DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 54
  DEFAULT CHARSET = latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `event_definitions`
--

LOCK TABLES `event_definitions` WRITE;
/*!40000 ALTER TABLE `event_definitions`
    DISABLE KEYS */;
INSERT INTO `event_definitions`
VALUES (-6, 0, 3, 'Low Average Fuel', 1, 30, '[\"Low Fuel\"]',
        '{\"text\" : \"Average fuel the past 15 seconds was less than 17.56\"}', '[\"Low Fuel\"]', 'min', 'null'),
       (-5, 0, 2, 'Low Average Fuel', 1, 30, '[\"Low Fuel\"]',
        '{\"text\" : \"Average fuel the past 15 seconds was less than 8.00\"}', '[\"Low Fuel\"]', 'min', 'null'),
       (-4, 0, 1, 'Low Average Fuel', 1, 30, '[\"Low Fuel\"]',
        '{\"text\" : \"Average fuel the past 15 seconds was less than 8.25\"}', '[\"Low Fuel\"]', 'min', 'null'),
       (-3, 0, 0, 'High Altitude Spin', 1, 30, '[\"NormAc\", \"AltAGL\", \"VSpd\", \"IAS\"]',
        '{\"text\" : \"Spin at or above 4,000\' AGL\"}', '[\"NormAc\"]', 'min', NULL),
       (-2, 0, 0, 'Low Altitude Spin', 1, 30, '[\"NormAc\", \"AltAGL\", \"VSpd\", \"IAS\"]',
        '{\"text\" : \"Spin below 4,000\' AGL\"}', '[\"NormAc\"]', 'min', NULL),
       (-1, 0, 0, 'Proximity', 1, 30,
        '[\"AltAGL\", \"AltMSL\", \"Latitude\", \"Longitude\", \"Lcl Date\", \"Lcl Time\", \"UTCOfst\"]',
        '{\"text\" : \"Aircraft within 500 ft of another aircraft and above above 50ft AGL\"}', '[]', 'min', 'null'),
       (1, 0, 0, 'Low Pitch', 1, 30, '[\"Pitch\"]',
        '{\"type\":\"GROUP\",\"condition\":\"AND\",\"filters\":[{\"type\":\"RULE\",\"inputs\":[\"Pitch\",\"<\",\"-30\"]}]}',
        '[\"Pitch\"]', 'max', NULL),
       (2, 0, 0, 'High Pitch', 1, 30, '[\"Pitch\"]',
        '{\"type\":\"GROUP\",\"condition\":\"AND\",\"filters\":[{\"type\":\"RULE\",\"inputs\":[\"Pitch\",\">\",\"30\"]}]}',
        '[\"Pitch\"]', 'max', NULL),
       (3, 0, 0, 'Low Lateral Acceleration', 1, 30, '[\"LatAc\"]',
        '{\"type\":\"GROUP\",\"condition\":\"AND\",\"filters\":[{\"type\":\"RULE\",\"inputs\":[\"LatAc\",\"<\",\"-1.33\"]}]}',
        '[\"LatAc\"]', 'max', NULL),
       (4, 0, 0, 'High Lateral Acceleration', 1, 30, '[\"LatAc\"]',
        '{\"type\":\"GROUP\",\"condition\":\"AND\",\"filters\":[{\"type\":\"RULE\",\"inputs\":[\"LatAc\",\">\",\"1.33\"]}]}',
        '[\"LatAc\"]', 'max', NULL),
       (5, 0, 0, 'Low Vertical Acceleration', 1, 30, '[\"NormAc\"]',
        '{\"type\":\"GROUP\",\"condition\":\"AND\",\"filters\":[{\"type\":\"RULE\",\"inputs\":[\"NormAc\",\"<\",\"-2.52\"]}]}',
        '[\"NormAc\"]', 'max', NULL),
       (6, 0, 0, 'High Vertical Acceleration', 1, 30, '[\"NormAc\"]',
        '{\"type\":\"GROUP\",\"condition\":\"AND\",\"filters\":[{\"type\":\"RULE\",\"inputs\":[\"NormAc\",\">\",\"2.8\"]}]}',
        '[\"NormAc\"]', 'max', NULL),
       (7, 0, 0, 'Roll', 1, 30, '[\"Roll\"]',
        '{\"type\":\"GROUP\",\"condition\":\"OR\",\"filters\":[{\"type\":\"RULE\",\"inputs\":[\"Roll\",\"<\",\"-60\"]},{\"type\":\"RULE\",\"inputs\":[\"Roll\",\">\",\"60\"]}]}',
        '[\"Roll\"]', 'max abs', NULL),
       (8, 0, 0, 'VSI on Final', 1, 30, '[\"VSpd\",\"AltAGL\"]',
        '{\"type\":\"GROUP\",\"condition\":\"AND\",\"filters\":[{\"type\":\"RULE\",\"inputs\":[\"VSpd\",\"<=\",\"-1500\"]},{\"type\":\"RULE\",\"inputs\":[\"AltAGL\",\"<=\",\"500\"]}]}',
        '[\"VSpd\"]', 'min', NULL),
       (9, 0, 2, 'Airspeed', 1, 30, '[\"IAS\"]',
        '{\"type\":\"GROUP\",\"condition\":\"AND\",\"filters\":[{\"type\":\"RULE\",\"inputs\":[\"IAS\",\">\",\"163\"]}]}',
        '[\"IAS\"]', 'max', NULL),
       (11, 0, 1, 'Airspeed', 1, 30, '[\"IAS\"]',
        '{\"type\":\"GROUP\",\"condition\":\"AND\",\"filters\":[{\"type\":\"RULE\",\"inputs\":[\"IAS\",\">\",\"154\"]}]}',
        '[\"IAS\"]', 'max', NULL),
       (12, 0, 3, 'Airspeed', 1, 30, '[\"IAS\"]',
        '{\"type\":\"GROUP\",\"condition\":\"AND\",\"filters\":[{\"type\":\"RULE\",\"inputs\":[\"IAS\",\">\",\"202\"]}]}',
        '[\"IAS\"]', 'max', NULL),
       (13, 0, 4, 'Airspeed', 1, 30, '[\"IAS\"]',
        '{\"type\":\"GROUP\",\"condition\":\"AND\",\"filters\":[{\"type\":\"RULE\",\"inputs\":[\"IAS\",\">\",\"200\"]}]}',
        '[\"IAS\"]', 'max', NULL),
       (14, 0, 2, 'Altitude', 1, 30, '[\"AltMSL\"]',
        '{\"type\":\"GROUP\",\"condition\":\"AND\",\"filters\":[{\"type\":\"RULE\",\"inputs\":[\"AltMSL\",\">\",\"12800\"]}]}',
        '[\"AltMSL\"]', 'max', NULL),
       (15, 0, 4, 'Altitude', 1, 30, '[\"AltMSL\"]',
        '{\"type\":\"GROUP\",\"condition\":\"AND\",\"filters\":[{\"type\":\"RULE\",\"inputs\":[\"AltMSL\",\">\",\"12800\"]}]}',
        '[\"AltMSL\"]', 'max', NULL),
       (16, 0, 1, 'Altitude', 1, 30, '[\"AltMSL\"]',
        '{\"type\":\"GROUP\",\"condition\":\"AND\",\"filters\":[{\"type\":\"RULE\",\"inputs\":[\"AltMSL\",\">\",\"12800\"]}]}',
        '[\"AltMSL\"]', 'max', NULL),
       (17, 0, 3, 'Altitude', 1, 30, '[\"AltMSL\"]',
        '{\"type\":\"GROUP\",\"condition\":\"AND\",\"filters\":[{\"type\":\"RULE\",\"inputs\":[\"AltMSL\",\">\",\"12800\"]}]}',
        '[\"AltMSL\"]', 'max', NULL),
       (21, 0, 2, 'Cylinder Head Temperature', 1, 30, '[\"E1 CHT4\",\"E1 CHT3\",\"E1 CHT2\",\"E1 CHT1\"]',
        '{\"type\":\"GROUP\",\"condition\":\"OR\",\"filters\":[{\"type\":\"RULE\",\"inputs\":[\"E1 CHT1\",\">\",\"500\"]},{\"type\":\"RULE\",\"inputs\":[\"E1 CHT2\",\">\",\"500\"]},{\"type\":\"RULE\",\"inputs\":[\"E1 CHT3\",\">\",\"500\"]},{\"type\":\"RULE\",\"inputs\":[\"E1 CHT4\",\">\",\"500\"]}]}',
        '[\"E1 CHT4\",\"E1 CHT3\",\"E1 CHT2\",\"E1 CHT1\"]', 'max', NULL),
       (22, 0, 4, 'Cylinder Head Temperature', 1, 30,
        '[\"E1 CHT4\",\"E1 CHT3\",\"E1 CHT6\",\"E1 CHT5\",\"E1 CHT2\",\"E1 CHT1\"]',
        '{\"type\":\"GROUP\",\"condition\":\"OR\",\"filters\":[{\"type\":\"RULE\",\"inputs\":[\"E1 CHT1\",\">\",\"460\"]},{\"type\":\"RULE\",\"inputs\":[\"E1 CHT2\",\">\",\"460\"]},{\"type\":\"RULE\",\"inputs\":[\"E1 CHT3\",\">\",\"460\"]},{\"type\":\"RULE\",\"inputs\":[\"E1 CHT4\",\">\",\"460\"]},{\"type\":\"RULE\",\"inputs\":[\"E1 CHT5\",\">\",\"460\"]},{\"type\":\"RULE\",\"inputs\":[\"E1 CHT6\",\">\",\"460\"]}]}',
        '[\"E1 CHT4\",\"E1 CHT3\",\"E1 CHT6\",\"E1 CHT5\",\"E1 CHT2\",\"E1 CHT1\"]', 'max', NULL),
       (23, 0, 3, 'Cylinder Head Temperature', 1, 30, '[\"E1 CHT1\",\"E2 CHT1\"]',
        '{\"type\":\"GROUP\",\"condition\":\"OR\",\"filters\":[{\"type\":\"RULE\",\"inputs\":[\"E1 CHT1\",\">\",\"500\"]},{\"type\":\"RULE\",\"inputs\":[\"E2 CHT1\",\">\",\"500\"]}]}',
        '[\"E1 CHT1\",\"E2 CHT1\"]', 'max', NULL),
       (24, 0, 2, 'Low Oil Pressure', 1, 30, '[\"E1 OilP\",\"E1 RPM\"]',
        '{\"type\":\"GROUP\",\"condition\":\"AND\",\"filters\":[{\"type\":\"RULE\",\"inputs\":[\"E1 OilP\",\"<\",\"20\"]},{\"type\":\"RULE\",\"inputs\":[\"E1 RPM\",\">\",\"500\"]}]}',
        '[\"E1 OilP\"]', 'min', NULL),
       (25, 0, 4, 'Low Oil Pressure', 1, 30, '[\"E1 OilP\",\"E1 RPM\"]',
        '{\"type\":\"GROUP\",\"condition\":\"AND\",\"filters\":[{\"type\":\"RULE\",\"inputs\":[\"E1 OilP\",\"<\",\"10\"]},{\"type\":\"RULE\",\"inputs\":[\"E1 RPM\",\">\",\"500\"]}]}',
        '[\"E1 OilP\"]', 'min', NULL),
       (26, 0, 1, 'Low Oil Pressure', 1, 30, '[\"E1 OilP\",\"E1 RPM\"]',
        '{\"type\":\"GROUP\",\"condition\":\"AND\",\"filters\":[{\"type\":\"RULE\",\"inputs\":[\"E1 OilP\",\"<\",\"25\"]},{\"type\":\"RULE\",\"inputs\":[\"E1 RPM\",\">\",\"500\"]}]}',
        '[\"E1 OilP\"]', 'min', NULL),
       (27, 0, 3, 'Low Oil Pressure', 1, 30, '[\"E1 OilP\",\"E2 RPM\",\"E2 OilP\",\"E1 RPM\"]',
        '{\"type\":\"GROUP\",\"condition\":\"OR\",\"filters\":[{\"type\":\"GROUP\",\"condition\":\"AND\",\"filters\":[{\"type\":\"RULE\",\"inputs\":[\"E1 OilP\",\"<\",\"25\"]},{\"type\":\"RULE\",\"inputs\":[\"E1 RPM\",\">\",\"500\"]}]},{\"type\":\"GROUP\",\"condition\":\"AND\",\"filters\":[{\"type\":\"RULE\",\"inputs\":[\"E2 OilP\",\"<\",\"25\"]},{\"type\":\"RULE\",\"inputs\":[\"E2 RPM\",\">\",\"500\"]}]}]}',
        '[\"E1 OilP\",\"E2 OilP\"]', 'min', NULL),
       (28, 0, 2, 'Low Fuel', 1, 30, '[\"Pitch\",\"Total Fuel\"]',
        '{\"type\":\"GROUP\",\"condition\":\"AND\",\"filters\":[{\"type\":\"RULE\",\"inputs\":[\"Total Fuel\",\"<\",\"8.0\"]},{\"type\":\"RULE\",\"inputs\":[\"Pitch\",\"<\",\"5\"]}]}',
        '[\"Total Fuel\"]', 'min', NULL),
       (29, 0, 1, 'Low Fuel', 1, 30, '[\"Pitch\",\"Total Fuel\"]',
        '{\"type\":\"GROUP\",\"condition\":\"AND\",\"filters\":[{\"type\":\"RULE\",\"inputs\":[\"Total Fuel\",\"<\",\"8.25\"]},{\"type\":\"RULE\",\"inputs\":[\"Pitch\",\"<\",\"5\"]}]}',
        '[\"Total Fuel\"]', 'min', NULL),
       (30, 0, 3, 'Low Fuel', 1, 30, '[\"Pitch\",\"Total Fuel\"]',
        '{\"type\":\"GROUP\",\"condition\":\"AND\",\"filters\":[{\"type\":\"RULE\",\"inputs\":[\"Total Fuel\",\"<\",\"17.56\"]},{\"type\":\"RULE\",\"inputs\":[\"Pitch\",\"<\",\"5\"]}]}',
        '[\"Total Fuel\"]', 'min', NULL),
       (31, 0, 2, 'Low Airspeed on Approach', 1, 30, '[\"AltMSL Lag Diff\",\"IAS\",\"AltAGL\"]',
        '{\"type\":\"GROUP\",\"condition\":\"AND\",\"filters\":[{\"type\":\"RULE\",\"inputs\":[\"IAS\",\"<\",\"57\"]},{\"type\":\"RULE\",\"inputs\":[\"AltMSL Lag Diff\",\"<\",\"0\"]},{\"type\":\"RULE\",\"inputs\":[\"AltAGL\",\">\",\"100\"]},{\"type\":\"RULE\",\"inputs\":[\"AltAGL\",\"<\",\"500\"]}]}',
        '[\"IAS\"]', 'min', NULL),
       (32, 0, 1, 'Low Airspeed on Approach', 1, 30, '[\"AltMSL Lag Diff\",\"IAS\",\"AltAGL\"]',
        '{\"type\":\"GROUP\",\"condition\":\"AND\",\"filters\":[{\"type\":\"RULE\",\"inputs\":[\"IAS\",\"<\",\"56\"]},{\"type\":\"RULE\",\"inputs\":[\"AltMSL Lag Diff\",\"<\",\"0\"]},{\"type\":\"RULE\",\"inputs\":[\"AltAGL\",\">\",\"100\"]},{\"type\":\"RULE\",\"inputs\":[\"AltAGL\",\"<\",\"500\"]}]}',
        '[\"IAS\"]', 'min', NULL),
       (33, 0, 3, 'Low Airspeed on Approach', 1, 30, '[\"AltMSL Lag Diff\",\"IAS\",\"AltAGL\"]',
        '{\"type\":\"GROUP\",\"condition\":\"AND\",\"filters\":[{\"type\":\"RULE\",\"inputs\":[\"IAS\",\"<\",\"66\"]},{\"type\":\"RULE\",\"inputs\":[\"AltMSL Lag Diff\",\"<\",\"0\"]},{\"type\":\"RULE\",\"inputs\":[\"AltAGL\",\">\",\"100\"]},{\"type\":\"RULE\",\"inputs\":[\"AltAGL\",\"<\",\"500\"]}]}',
        '[\"IAS\"]', 'min', NULL),
       (34, 0, 4, 'Low Airspeed on Approach', 1, 30, '[\"AltMSL Lag Diff\",\"IAS\",\"AltAGL\"]',
        '{\"type\":\"GROUP\",\"condition\":\"AND\",\"filters\":[{\"type\":\"RULE\",\"inputs\":[\"IAS\",\"<\",\"67\"]},{\"type\":\"RULE\",\"inputs\":[\"AltMSL Lag Diff\",\"<\",\"0\"]},{\"type\":\"RULE\",\"inputs\":[\"AltAGL\",\">\",\"100\"]},{\"type\":\"RULE\",\"inputs\":[\"AltAGL\",\"<\",\"500\"]}]}',
        '[\"IAS\"]', 'min', NULL),
       (35, 0, 2, 'Low Airspeed on Climbout', 1, 30, '[\"AltMSL Lag Diff\",\"IAS\",\"AltAGL\"]',
        '{\"type\":\"GROUP\",\"condition\":\"AND\",\"filters\":[{\"type\":\"RULE\",\"inputs\":[\"IAS\",\">=\",\"20\"]},{\"type\":\"RULE\",\"inputs\":[\"IAS\",\"<\",\"52\"]},{\"type\":\"RULE\",\"inputs\":[\"AltMSL Lag Diff\",\">\",\"0\"]},{\"type\":\"RULE\",\"inputs\":[\"AltAGL\",\">\",\"100\"]},{\"type\":\"RULE\",\"inputs\":[\"AltAGL\",\"<\",\"500\"]}]}',
        '[\"IAS\"]', 'min', NULL),
       (36, 0, 1, 'Low Airspeed on Climbout', 1, 30, '[\"AltMSL Lag Diff\",\"IAS\",\"AltAGL\"]',
        '{\"type\":\"GROUP\",\"condition\":\"AND\",\"filters\":[{\"type\":\"RULE\",\"inputs\":[\"IAS\",\">=\",\"20\"]},{\"type\":\"RULE\",\"inputs\":[\"IAS\",\"<\",\"59\"]},{\"type\":\"RULE\",\"inputs\":[\"AltMSL Lag Diff\",\">\",\"0\"]},{\"type\":\"RULE\",\"inputs\":[\"AltAGL\",\">\",\"100\"]},{\"type\":\"RULE\",\"inputs\":[\"AltAGL\",\"<\",\"500\"]}]}',
        '[\"IAS\"]', 'min', NULL),
       (37, 0, 3, 'Low Airspeed on Climbout', 1, 30, '[\"AltMSL Lag Diff\",\"IAS\",\"AltAGL\"]',
        '{\"type\":\"GROUP\",\"condition\":\"AND\",\"filters\":[{\"type\":\"RULE\",\"inputs\":[\"IAS\",\">=\",\"20\"]},{\"type\":\"RULE\",\"inputs\":[\"IAS\",\"<\",\"70\"]},{\"type\":\"RULE\",\"inputs\":[\"AltMSL Lag Diff\",\">\",\"0\"]},{\"type\":\"RULE\",\"inputs\":[\"AltAGL\",\">\",\"100\"]},{\"type\":\"RULE\",\"inputs\":[\"AltAGL\",\"<\",\"500\"]}]}',
        '[\"IAS\"]', 'min', NULL),
       (38, 0, 4, 'Low Airspeed on Climbout', 1, 30, '[\"AltMSL Lag Diff\",\"IAS\",\"AltAGL\"]',
        '{\"type\":\"GROUP\",\"condition\":\"AND\",\"filters\":[{\"type\":\"RULE\",\"inputs\":[\"IAS\",\">=\",\"20\"]},{\"type\":\"RULE\",\"inputs\":[\"IAS\",\"<\",\"60\"]},{\"type\":\"RULE\",\"inputs\":[\"AltMSL Lag Diff\",\">\",\"0\"]},{\"type\":\"RULE\",\"inputs\":[\"AltAGL\",\">\",\"100\"]},{\"type\":\"RULE\",\"inputs\":[\"AltAGL\",\"<\",\"500\"]}]}',
        '[\"IAS\"]', 'min', NULL),
       (39, 0, 2, 'CHT Sensor Divergence', 1, 30, '[\"E1 CHT Variance\"]',
        '{\"type\":\"GROUP\",\"condition\":\"AND\",\"filters\":[{\"type\":\"RULE\",\"inputs\":[\"E1 CHT Divergence\",\">\",\"100\"]}]}',
        '[\"E1 CHT Variance\"]', 'max', NULL),
       (40, 0, 1, 'CHT Sensor Divergence', 1, 30, '[\"E1 CHT Variance\"]',
        '{\"type\":\"GROUP\",\"condition\":\"AND\",\"filters\":[{\"type\":\"RULE\",\"inputs\":[\"E1 CHT Divergence\",\">\",\"100\"]}]}',
        '[\"E1 CHT Variance\"]', 'max', NULL),
       (41, 0, 4, 'CHT Sensor Divergence', 1, 30, '[\"E1 CHT Variance\"]',
        '{\"type\":\"GROUP\",\"condition\":\"AND\",\"filters\":[{\"type\":\"RULE\",\"inputs\":[\"E1 CHT Divergence\",\">\",\"100\"]}]}',
        '[\"E1 CHT Variance\"]', 'max', NULL),
       (42, 0, 2, 'EGT Sensor Divergence', 1, 30, '[\"E1 EGT Variance\"]',
        '{\"type\":\"GROUP\",\"condition\":\"OR\",\"filters\":[{\"type\":\"RULE\",\"inputs\":[\"E1 EGT Divergence\",\">\",\"400\"]}]}',
        '[\"E1 EGT Variance\"]', 'max', NULL),
       (43, 0, 4, 'EGT Sensor Divergence', 1, 30, '[\"E1 EGT Variance\"]',
        '{\"type\":\"GROUP\",\"condition\":\"OR\",\"filters\":[{\"type\":\"RULE\",\"inputs\":[\"E1 EGT Divergence\",\">\",\"400\"]}]}',
        '[\"E1 EGT Variance\"]', 'max', NULL),
       (44, 0, 1, 'EGT Sensor Divergence', 1, 30, '[\"E1 EGT Variance\"]',
        '{\"type\":\"GROUP\",\"condition\":\"OR\",\"filters\":[{\"type\":\"RULE\",\"inputs\":[\"E1 EGT Divergence\",\">\",\"400\"]}]}',
        '[\"E1 EGT Variance\"]', 'max', NULL),
       (45, 0, 3, 'EGT Sensor Divergence', 1, 30, '[\"E2 EGT Variance\",\"E1 EGT Variance\"]',
        '{\"type\":\"GROUP\",\"condition\":\"OR\",\"filters\":[{\"type\":\"RULE\",\"inputs\":[\"E1 EGT Divergence\",\">\",\"400\"]},{\"type\":\"RULE\",\"inputs\":[\"E2 EGT Divergence\",\">\",\"400\"]}]}',
        '[\"E2 EGT Variance\",\"E1 EGT Variance\"]', 'max', NULL),
       (46, 0, 2, 'Engine Shutdown Below 3000 Ft', 1, 30, '[\"E1 RPM\",\"AltAGL\"]',
        '{\"type\":\"GROUP\",\"condition\":\"AND\",\"filters\":[{\"type\":\"RULE\",\"inputs\":[\"E1 RPM\",\"<\",\"100\"]},{\"type\":\"RULE\",\"inputs\":[\"AltAGL\",\">\",\"500\"]},{\"type\":\"RULE\",\"inputs\":[\"AltAGL\",\"<\",\"3000\"]}]}',
        '[\"AltAGL\"]', 'min', NULL),
       (47, 0, 1, 'Engine Shutdown Below 3000 Ft', 1, 30, '[\"E1 RPM\",\"AltAGL\"]',
        '{\"type\":\"GROUP\",\"condition\":\"AND\",\"filters\":[{\"type\":\"RULE\",\"inputs\":[\"E1 RPM\",\"<\",\"100\"]},{\"type\":\"RULE\",\"inputs\":[\"AltAGL\",\">\",\"500\"]},{\"type\":\"RULE\",\"inputs\":[\"AltAGL\",\"<\",\"3000\"]}]}',
        '[\"AltAGL\"]', 'min', NULL),
       (48, 0, 4, 'Engine Shutdown Below 3000 Ft', 1, 30, '[\"E1 RPM\",\"AltAGL\"]',
        '{\"type\":\"GROUP\",\"condition\":\"AND\",\"filters\":[{\"type\":\"RULE\",\"inputs\":[\"E1 RPM\",\"<\",\"100\"]},{\"type\":\"RULE\",\"inputs\":[\"AltAGL\",\">\",\"500\"]},{\"type\":\"RULE\",\"inputs\":[\"AltAGL\",\"<\",\"3000\"]}]}',
        '[\"AltAGL\"]', 'min', NULL),
       (49, 0, 3, 'Engine Shutdown Below 3000 Ft', 1, 30, '[\"E2 RPM\",\"E1 RPM\",\"AltAGL\"]',
        '{\"type\":\"GROUP\",\"condition\":\"AND\",\"filters\":[{\"type\":\"RULE\",\"inputs\":[\"AltAGL\",\">\",\"500\"]},{\"type\":\"RULE\",\"inputs\":[\"AltAGL\",\"<\",\"3000\"]},{\"type\":\"GROUP\",\"condition\":\"OR\",\"filters\":[{\"type\":\"RULE\",\"inputs\":[\"E1 RPM\",\"<\",\"100\"]},{\"type\":\"RULE\",\"inputs\":[\"E2 RPM\",\"<\",\"100\"]}]}]}',
        '[\"AltAGL\"]', 'min', NULL),
       (50, 0, 2, 'High Altitude LOC-I', 2, 1,
        '[\"AltAGL\",\"LOC-I Index\",\"Coordination Index\",\"Yaw Rate\",\"AltB\"]',
        '{\"type\":\"GROUP\",\"condition\":\"AND\",\"filters\":[{\"type\":\"RULE\",\"inputs\":[\"LOC-I Index\",\">=\",\"1\"]},{\"type\":\"RULE\",\"inputs\":[\"AltAGL\",\">=\",\"1500\"]}]}',
        '[\"Yaw Rate\", \"AltAGL\"]', 'max', NULL),
       (51, 0, 0, 'High Altitude Stall', 2, 1,
        '[\"AltAGL\",\"Stall Index\",\"Coordination Index\",\"AOASimple\",\"AltB\"]',
        '{\"type\":\"GROUP\",\"condition\":\"AND\",\"filters\":[{\"type\":\"RULE\",\"inputs\":[\"Stall Index\",\">=\",\"1\"]},{\"type\":\"RULE\",\"inputs\":[\"AltAGL\",\">=\",\"1500\"]}]}',
        '[\"AOASimple\", \"AltAGL\"]', 'max', NULL),
       (52, 0, 0, 'Low Altitude Stall', 2, 1,
        '[\"AltAGL\",\"Stall Index\",\"Coordination Index\",\"AOASimple\",\"AltB\"]',
        '{\"type\":\"GROUP\",\"condition\":\"AND\",\"filters\":[{\"type\":\"RULE\",\"inputs\":[\"Stall Index\",\">=\",\"1\"]},{\"type\":\"RULE\",\"inputs\":[\"AltAGL\",\"<=\",\"1500\"]},{\"type\":\"RULE\",\"inputs\":[\"AltAGL\",\">=\",\"100\"]}]}',
        '[\"AOASimple\", \"AltAGL\"]', 'max', NULL),
       (53, 0, 2, 'Low Altitude LOC-I', 2, 1,
        '[\"AltAGL\",\"LOC-I Index\",\"Coordination Index\",\"Yaw Rate\",\"AltB\"]',
        '{\"type\":\"GROUP\",\"condition\":\"AND\",\"filters\":[{\"type\":\"RULE\",\"inputs\":[\"LOC-I Index\",\">=\",\"1\"]},{\"type\":\"RULE\",\"inputs\":[\"AltAGL\",\"<=\",\"1500\"]},{\"type\":\"RULE\",\"inputs\":[\"AltAGL\",\">=\",\"100\"]}]}',
        '[\"Yaw Rate\", \"AltAGL\"]', 'max', NULL);
/*!40000 ALTER TABLE `event_definitions`
    ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE = @OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE = @OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS = @OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS = @OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT = @OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS = @OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION = @OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES = @OLD_SQL_NOTES */;

-- Dump completed on 2022-07-28 12:35:49
