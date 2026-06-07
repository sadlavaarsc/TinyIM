package com.tinyim.message;

import com.tinyim.entity.Message;
import com.tinyim.gateway.ChannelManager;
import com.tinyim.offline.OfflineMessageService;
import com.tinyim.push.PushService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 消息服务
 * 提供消息发送接口（单聊、群聊），协调消息投递、落库、ACK确认及离线存储
 */
@Slf4j
@Service
public class MessageService {

    @Autowired
    private MessageProducer messageProducer;

    @Autowired
    private ChannelManager channelManager;

    @Autowired
    private OfflineMessageService offlineMessageService;

    @Autowired
    private PushService pushService;

    /**
     * 发送单聊消息
     * 消息先写入 WAL，再投递到 RocketMQ，保证至少一次投递
     *
     * @param message 消息对象
     * @return 消息ID
     */
    public Long sendPrivateMessage(Message message) {
        message.setMsgType(1);
        // 1. 写入 WAL，保证消息不丢失
        WALLogger.write("SEND_PRIVATE", message);

        // 2. 投递到消息队列
        messageProducer.sendMessage(message);

        log.info("单聊消息已投递, from={}, to={}", message.getFromUserId(), message.getToUserId());
        return message.getId();
    }

    /**
     * 发送群聊消息
     * 消息投递到 RocketMQ，由消费者进行群成员广播
     *
     * @param message 消息对象（需包含 groupId）
     * @return 消息ID
     */
    public Long sendGroupMessage(Message message) {
        message.setMsgType(2);
        WALLogger.write("SEND_GROUP", message);

        messageProducer.sendMessage(message);

        log.info("群聊消息已投递, from={}, groupId={}", message.getFromUserId(), message.getGroupId());
        return message.getId();
    }

    /**
     * 处理消息送达确认（ACK）
     * 客户端收到消息后发送 ACK，服务端更新消息状态
     *
     * @param messageId 消息ID
     * @param userId    用户ID
     */
    public void handleAck(Long messageId, Long userId) {
        log.info("收到ACK, messageId={}, userId={}", messageId, userId);
        // TODO: 更新消息状态为已送达/已读，清除重试定时器
    }

    /**
     * 消息重试
     * 当消息未收到 ACK 且超时时，触发重试投递
     *
     * @param message 消息对象
     */
    public void retryMessage(Message message) {
        log.warn("消息重试, messageId={}, toUserId={}", message.getId(), message.getToUserId());
        if (channelManager.isOnline(message.getToUserId())) {
            pushService.pushToUser(message.getToUserId(), message);
        } else {
            offlineMessageService.storeOfflineMessage(message);
        }
    }
}
