package com.tinyim.message;

import com.alibaba.fastjson2.JSON;
import com.tinyim.entity.Message;
import com.tinyim.gateway.ChannelManager;
import com.tinyim.offline.OfflineMessageService;
import com.tinyim.push.PushService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 消息消费者
 * 从 RocketMQ 订阅消息，根据接收方在线状态决定推送到客户端或存入离线消息库
 */
@Slf4j
@Component
@RocketMQMessageListener(
        topic = "${im.mq.topic:message-topic}",
        consumerGroup = "${im.mq.consumer.group:message-consumer-group}"
)
public class MessageConsumer implements RocketMQListener<String> {

    @Autowired
    private ChannelManager channelManager;

    @Autowired
    private PushService pushService;

    @Autowired
    private OfflineMessageService offlineMessageService;

    /**
     * 消费消息
     * 解析消息内容，判断接收方是否在线，在线则推送，离线则存储
     *
     * @param message 消息 JSON 字符串
     */
    @Override
    public void onMessage(String message) {
        try {
            Message msg = JSON.parseObject(message, Message.class);
            log.info("消费消息, messageId={}, from={}, to={}", msg.getId(), msg.getFromUserId(), msg.getToUserId());

            if (msg.getMsgType() != null && msg.getMsgType() == 1) {
                // 单聊消息
                handlePrivateMessage(msg);
            } else if (msg.getMsgType() != null && msg.getMsgType() == 2) {
                // 群聊消息
                handleGroupMessage(msg);
            } else {
                log.warn("未知消息类型, messageId={}", msg.getId());
            }
        } catch (Exception e) {
            log.error("消息消费异常, payload={}", message, e);
        }
    }

    /**
     * 处理单聊消息
     *
     * @param message 消息对象
     */
    private void handlePrivateMessage(Message message) {
        Long toUserId = message.getToUserId();
        if (channelManager.isOnline(toUserId)) {
            pushService.pushToUser(toUserId, message);
        } else {
            offlineMessageService.storeOfflineMessage(message);
        }
    }

    /**
     * 处理群聊消息
     * 遍历群成员，逐个判断在线状态并推送或离线存储
     *
     * @param message 消息对象
     */
    private void handleGroupMessage(Message message) {
        // TODO: 从群组服务获取群成员列表，此处简化处理
        Long groupId = message.getGroupId();
        log.info("群聊消息, groupId={}, messageId={}", groupId, message.getId());
        // 实际场景：获取群成员列表后逐个推送
    }
}
