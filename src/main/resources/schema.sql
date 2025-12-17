-- 删除已存在的表，按依赖顺序
-- 先禁用外键检查以避免删除顺序问题
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `repair_feedback`;
DROP TABLE IF EXISTS `repair_order_image`;
DROP TABLE IF EXISTS `repair_order_status_log`;
DROP TABLE IF EXISTS `repair_order`;
DROP TABLE IF EXISTS `repair_category`;
DROP TABLE IF EXISTS `sys_user`;

-- 重新启用外键检查
SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE `sys_user` (
    `user_number` VARCHAR(255) PRIMARY KEY,
    `name` VARCHAR(100) NOT NULL,
    `password` VARCHAR(255) NOT NULL,
    `role` VARCHAR(20) NOT NULL,
    `enabled` BOOLEAN DEFAULT TRUE,
    `phone` VARCHAR(20),
    `created_at` DATETIME NOT NULL
);

CREATE TABLE `repair_category` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `category_key` VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE `repair_order` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `student_number` VARCHAR(255) NOT NULL,
    `category_key` VARCHAR(50),
    `repairman_id` VARCHAR(255),
    `status` VARCHAR(30) NOT NULL,
    `location` VARCHAR(100) NOT NULL,
    `description` TEXT,
    `rejection_reason` TEXT,
    `priority` VARCHAR(10) DEFAULT 'medium',
    `repair_notes` TEXT,
    `process_notes` TEXT,
    `estimated_completion_time` DATETIME,
    `created_at` DATETIME NOT NULL,
    `assigned_at` DATETIME,
    `completed_at` DATETIME,
    `closed_at` DATETIME,
    FOREIGN KEY (`student_number`) REFERENCES `sys_user`(`user_number`),
    FOREIGN KEY (`category_key`) REFERENCES `repair_category`(`category_key`),
    FOREIGN KEY (`repairman_id`) REFERENCES `sys_user`(`user_number`)
);

CREATE TABLE `repair_order_image` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `repair_order_id` BIGINT NOT NULL,
    `image_url` VARCHAR(500) NOT NULL,
    `created_at` DATETIME NOT NULL,
    FOREIGN KEY (`repair_order_id`) REFERENCES `repair_order`(`id`)
);

CREATE TABLE `repair_order_status_log` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `repair_order_id` BIGINT NOT NULL,
    `operate_user_id` VARCHAR(255),
    `before_status` VARCHAR(30),
    `after_status` VARCHAR(30) NOT NULL,
    `operate_time` DATETIME NOT NULL,
    FOREIGN KEY (`repair_order_id`) REFERENCES `repair_order`(`id`),
    FOREIGN KEY (`operate_user_id`) REFERENCES `sys_user`(`user_number`)
);

CREATE TABLE `repair_feedback` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `repair_order_id` BIGINT NOT NULL UNIQUE,
    `student_number` VARCHAR(255) NOT NULL,
    `repairman_id` VARCHAR(255) NOT NULL,
    `rating` INT NOT NULL CHECK (`rating` >= 1 AND `rating` <= 5),
    `comment` TEXT,
    `created_at` DATETIME NOT NULL,
    FOREIGN KEY (`repair_order_id`) REFERENCES `repair_order`(`id`),
    FOREIGN KEY (`student_number`) REFERENCES `sys_user`(`user_number`),
    FOREIGN KEY (`repairman_id`) REFERENCES `sys_user`(`user_number`)
);

-- 插入初始数据
INSERT INTO `repair_category` (`category_key`) VALUES 
('水电维修'),
('网络故障'),
('家具维修'),
('电器故障'),
('公共设施');

-- 插入初始数据
INSERT INTO `repair_category` (`category_key`) VALUES 
('水电维修'),
('网络故障'),
('家具维修'),
('电器故障'),
('公共设施');

-- 插入初始用户数据，密码为041206的BCRYPT哈希值
INSERT INTO `sys_user` (`user_number`, `name`, `password`, `role`, `enabled`, `phone`, `created_at`) VALUES
('admin', '管理员', '$2a$10$w9ziBssO/.lfsyOhdZRjzerP/Qhl3NryE/F9dmTGVGAhF2Vc7t/RG', 'ADMIN', TRUE, '13800000000', NOW()),
('worker001', '维修员张三', '$2a$10$w9ziBssO/.lfsyOhdZRjzerP/Qhl3NryE/F9dmTGVGAhF2Vc7t/RG', 'STAFF', TRUE, '13800000001', NOW()),
('worker002', '维修员李四', '$2a$10$w9ziBssO/.lfsyOhdZRjzerP/Qhl3NryE/F9dmTGVGAhF2Vc7t/RG', 'STAFF', TRUE, '13800000002', NOW()),
('20210001', '学生王五', '$2a$10$w9ziBssO/.lfsyOhdZRjzerP/Qhl3NryE/F9dmTGVGAhF2Vc7t/RG', 'STUDENT', TRUE, '13800000003', NOW()),
('20210002', '学生赵六', '$2a$10$w9ziBssO/.lfsyOhdZRjzerP/Qhl3NryE/F9dmTGVGAhF2Vc7t/RG', 'STUDENT', TRUE, '13800000004', NOW());

-- 如果需要重置所有测试用户的密码为041206，可以取消下面语句的注释
-- UPDATE `sys_user` SET `password` = '$2a$10$w9ziBssO/.lfsyOhdZRjzerP/Qhl3NryE/F9dmTGVGAhF2Vc7t/RG';