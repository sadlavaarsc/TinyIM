package com.tinyim.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 离线消息实体类
 * 当用户不在线时，消息会被暂存为离线消息，待用户上线后推送
 */
@Data
public class OfflineMessage {

    /** 离线消息唯一标识 */
    private Long id;

    /** 接收方用户ID */
    private Long userId;

    /** 发送方用户ID */
    private Long fromUserId;

    /** 消息内容 */
    private String content;

    /** 消息类型：1-单聊 2-群聊 */
    private Integer msgType;

    /** 消息序列号 */
    private Long sequence;

    /** 是否已推送：0-未推送 1-已推送 */
    private Integer pushed;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 推送时间 */
    private LocalDateTime pushTime;
}
