package com.tinyim.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体类
 * 表示系统中的注册用户，包含用户基本信息及账户状态
 */
@Data
public class User {

    /** 用户唯一标识 */
    private Long id;

    /** 用户名 */
    private String username;

    /** 昵称 */
    private String nickname;

    /** 密码（加密存储） */
    private String password;

    /** 头像URL */
    private String avatar;

    /** 用户状态：0-禁用 1-正常 */
    private Integer status;

    /** 最后登录时间 */
    private LocalDateTime lastLoginTime;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
