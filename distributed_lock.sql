/*
Navicat MySQL Data Transfer

Source Server         : centos
Source Server Version : 50716
Source Host           : 172.16.116.100:3306
Source Database       : distributed_lock

Target Server Type    : MYSQL
Target Server Version : 50716
File Encoding         : 65001

Date: 2022-07-25 21:44:16
*/

SET FOREIGN_KEY_CHECKS=0;

-- ----------------------------
-- Table structure for tb_lock
-- ----------------------------
DROP TABLE IF EXISTS `tb_lock`;
CREATE TABLE `tb_lock` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `lock_name` varchar(50) NOT NULL,
  `lock_time` datetime NOT NULL,
  `server_id` varchar(255) NOT NULL,
  `thread_id` int(11) NOT NULL,
  `count` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_unique` (`lock_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Records of tb_lock
-- ----------------------------

-- ----------------------------
-- Table structure for tb_stock
-- ----------------------------
DROP TABLE IF EXISTS `tb_stock`;
CREATE TABLE `tb_stock` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `product_code` varchar(20) NOT NULL,
  `warehouse` varchar(20) NOT NULL,
  `count` int(11) NOT NULL,
  `version` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_pc` (`product_code`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8;

-- ----------------------------
-- Records of tb_stock
-- ----------------------------
INSERT INTO `tb_stock` VALUES ('1', '1001', '北京仓', '0', '5009');
INSERT INTO `tb_stock` VALUES ('2', '1001', '上海仓', '4999', '0');
INSERT INTO `tb_stock` VALUES ('3', '1002', '深圳仓', '4997', '0');
INSERT INTO `tb_stock` VALUES ('4', '1002', '上海仓', '5000', '0');
