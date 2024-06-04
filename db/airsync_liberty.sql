-- MySQL dump 10.13  Distrib 5.7.42, for Linux (x86_64)
--
-- Host: localhost    Database: ngafid
-- ------------------------------------------------------
-- Server version   5.7.42
/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
--
-- Table structure for table `airsync_fleet_info`
--
DROP TABLE IF EXISTS `airsync_fleet_info`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `airsync_fleet_info` (
    `api_key` varchar(32) DEFAULT NULL,
  `api_secret` varchar(64) DEFAULT NULL,
  `last_upload_time` timestamp NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `timeout` int(11) DEFAULT NULL,
  `override` tinyint(4) DEFAULT '0',
  `mutex` tinyint(4) DEFAULT '0',
  KEY `airsync_fleet_id_fk` (`fleet_id`),
  CONSTRAINT `airsync_fleet_id_fk` FOREIGN KEY (`fleet_id`) REFERENCES `fleet` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;
--
-- Dumping data for table `airsync_fleet_info`
--
LOCK TABLES `airsync_fleet_info` WRITE;
/*!40000 ALTER TABLE `airsync_fleet_info` DISABLE KEYS */;
INSERT INTO `airsync_fleet_info` VALUES (13,'PY9K72SUW1R2G2HVMR8SQEB2LJN1MD16','E8MM7PR48PZWHRY7M5WJ2QX4VAVQ5RICT2ASIOK8UMVRJZM1EH5K3LQHW9G9SIXA','2023-06-28 16:28:27',30,0,0);
/*!40000 ALTER TABLE `airsync_fleet_info` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;
/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
-- Dump completed on 2023-06-28 12:52:41
