package com.tinyim.message;

import com.alibaba.fastjson2.JSON;
import com.tinyim.entity.Message;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

/**
 * 消息生产者
 * 负责将消息投递到 RocketMQ，实现异步解耦及削峰填谷，保证消息至少一次投递
 */
@Slf4j
@Component
public class MessageProducer {

    @Value("${im.mq.topic:message-topic}")
    private String messageTopic;

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    /**
     * 发送消息到 RocketMQ
     * 使用同步发送方式，确保消息可靠投递到 Broker
     *
     * @param message 消息对象
     */
    public void sendMessage(Message message) {
        try {
            String payload = JSON.toJSONString(message);
            org.springframework.messaging.Message<String> springMessage = MessageBuilder
                    .withPayload(payload)
                    .setHeader("messageId", message.getId())
                    .setHeader("fromUserId", message.getFromUserId())
                    .setHeader("toUserId", message.getToUserId())
                    .build();

            rocketMQTemplate.syncSend(messageTopic, springMessage);
            log.info("消息投递成功, messageId={}, topic={}", message.getId(), messageTopic);
        } catch (Exception e) {
            log.error("消息投递失败, messageId={}", message.getId(), e);
            // 投递失败可进入本地重试队列或告警
        }
    }

    /**
     * 发送顺序消息
     * 基于用户 ID 哈希选择队列，保证单聊消息严格有序
     *
     * @param message 消息对象
     * @param hashKey 哈希键（通常为用户ID）
     */
    public void sendOrderedMessage(Message message, String hashKey) {
        try {
            String payload = JSON.toJSONString(message);
            org.springframework.messaging.Message<String> springMessage = MessageBuilder
                    .withPayload(payload)
                    .setHeader("hashKey", hashKey)
                    .build();

            // 使用 hashKey 保证消息进入同一队列
            rocketMQTemplate.syncSendOrderly(messageTopic, springMessage, hashKey);
            log.info("顺序消息投递成功, messageId={}, hashKey={}", message.getId(), hashKey);
        } catch (Exception e) {
            log.error("顺序消息投递失败, messageId={}", message.getId(), e);
        }
    }
}
