-- ===============================
-- TinyIM 数据库初始化脚本
-- 包含用户表、消息表、离线消息表
-- ===============================

-- 创建数据库（如不存在）
CREATE DATABASE IF NOT EXISTS tinyim
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE tinyim;

-- ===============================
-- 用户表 (user)
-- ===============================
CREATE TABLE IF NOT EXISTS `user` (
    `id`            BIGINT UNSIGNED     NOT NULL AUTO_INCREMENT COMMENT '用户唯一标识',
    `username`      VARCHAR(64)         NOT NULL                COMMENT '用户名',
    `nickname`      VARCHAR(128)        DEFAULT NULL            COMMENT '昵称',
    `password`      VARCHAR(256)        NOT NULL                COMMENT '密码（加密存储）',
    `avatar`        VARCHAR(512)        DEFAULT NULL            COMMENT '头像URL',
    `status`        TINYINT             NOT NULL DEFAULT 1      COMMENT '用户状态：0-禁用 1-正常',
    `last_login_time` DATETIME          DEFAULT NULL            COMMENT '最后登录时间',
    `create_time`   DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ===============================
-- 消息表 (message)
-- ===============================
CREATE TABLE IF NOT EXISTS `message` (
    `id`            BIGINT UNSIGNED     NOT NULL AUTO_INCREMENT COMMENT '消息唯一标识',
    `from_user_id`  BIGINT UNSIGNED     NOT NULL                COMMENT '发送方用户ID',
    `to_user_id`    BIGINT UNSIGNED     DEFAULT NULL            COMMENT '接收方用户ID（单聊时有效）',
    `group_id`      BIGINT UNSIGNED     DEFAULT NULL            COMMENT '群组ID（群聊时有效）',
    `msg_type`      TINYINT             NOT NULL                COMMENT '消息类型：1-单聊 2-群聊 3-系统消息',
    `content_type`  TINYINT             NOT NULL DEFAULT 1      COMMENT '消息内容类型：1-文本 2-图片 3-语音 4-视频',
    `content`       TEXT                NOT NULL                COMMENT '消息内容',
    `status`        TINYINT             NOT NULL DEFAULT 0      COMMENT '消息状态：0-发送中 1-已送达 2-已读',
    `sequence`      BIGINT UNSIGNED     NOT NULL                COMMENT '消息序列号，用于保证消息顺序',
    `create_time`   DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_from_user_id` (`from_user_id`),
    KEY `idx_to_user_id` (`to_user_id`),
    KEY `idx_group_id` (`group_id`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_sequence` (`sequence`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息表';

-- ===============================
-- 离线消息表 (offline_message)
-- ===============================
CREATE TABLE IF NOT EXISTS `offline_message` (
    `id`            BIGINT UNSIGNED     NOT NULL AUTO_INCREMENT COMMENT '离线消息唯一标识',
    `user_id`       BIGINT UNSIGNED     NOT NULL                COMMENT '接收方用户ID',
    `from_user_id`  BIGINT UNSIGNED     NOT NULL                COMMENT '发送方用户ID',
    `content`       TEXT                NOT NULL                COMMENT '消息内容（JSON格式）',
    `msg_type`      TINYINT             NOT NULL                COMMENT '消息类型：1-单聊 2-群聊',
    `sequence`      BIGINT UNSIGNED     NOT NULL                COMMENT '消息序列号',
    `pushed`        TINYINT             NOT NULL DEFAULT 0      COMMENT '是否已推送：0-未推送 1-已推送',
    `create_time`   DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `push_time`     DATETIME            DEFAULT NULL            COMMENT '推送时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id_pushed` (`user_id`, `pushed`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_sequence` (`sequence`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='离线消息表';
