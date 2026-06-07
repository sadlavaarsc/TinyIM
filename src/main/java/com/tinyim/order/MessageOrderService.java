package com.tinyim.order;

import com.tinyim.entity.Message;
import com.tinyim.message.MessageProducer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 消息顺序服务
 * 基于用户 ID 哈希分片绑定单队列，保证单聊消息严格有序
 * 核心思想：相同会话的消息通过相同的 hashKey 路由到 RocketMQ 的同一队列中
 */
@Slf4j
@Service
public class MessageOrderService {

    @Autowired
    private MessageProducer messageProducer;

    /**
     * 发送有序单聊消息
     * 通过用户ID组合生成 hashKey，确保同一对用户的消息进入同一队列
     *
     * @param message 消息对象
     * @return 消息ID
     */
    public Long sendOrderedPrivateMessage(Message message) {
        message.setMsgType(1);
        String hashKey = generateHashKey(message.getFromUserId(), message.getToUserId());

        messageProducer.sendOrderedMessage(message, hashKey);
        log.info("有序单聊消息已投递, from={}, to={}, hashKey={}",
                message.getFromUserId(), message.getToUserId(), hashKey);
        return message.getId();
    }

    /**
     * 生成会话哈希键
     * 将两个用户ID按从小到大排序后拼接，保证双向消息进入同一队列
     *
     * @param userId1 用户ID1
     * @param userId2 用户ID2
     * @return 哈希键字符串
     */
    private String generateHashKey(Long userId1, Long userId2) {
        if (userId1 < userId2) {
            return userId1 + "_" + userId2;
        } else {
            return userId2 + "_" + userId1;
        }
    }
}
