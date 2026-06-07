package com.tinyim.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 消息实体类
 * 表示即时通讯系统中的一条消息，包含发送方、接收方、消息内容、类型及状态等信息
 */
@Data
public class Message {

    /** 消息唯一标识 */
    private Long id;

    /** 发送方用户ID */
    private Long fromUserId;

    /** 接收方用户ID（单聊时有效） */
    private Long toUserId;

    /** 群组ID（群聊时有效） */
    private Long groupId;

    /** 消息类型：1-单聊 2-群聊 3-系统消息 */
    private Integer msgType;

    /** 消息内容类型：1-文本 2-图片 3-语音 4-视频 */
    private Integer contentType;

    /** 消息内容 */
    private String content;

    /** 消息状态：0-发送中 1-已送达 2-已读 */
    private Integer status;

    /** 消息序列号，用于保证消息顺序 */
    private Long sequence;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
