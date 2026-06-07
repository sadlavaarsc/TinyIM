package com.tinyim.push;

import com.alibaba.fastjson2.JSON;
import com.tinyim.entity.Message;
import com.tinyim.gateway.ChannelManager;
import com.tinyim.offline.OfflineMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;

/**
 * 推送服务
 * 负责消息路由、单用户推送、系统广播及在线/离线决策
 */
@Slf4j
@Service
public class PushService {

    @Autowired
    private ChannelManager channelManager;

    @Autowired
    private OfflineMessageService offlineMessageService;

    /**
     * 消息路由入口
     * 根据消息类型（单聊/群聊/系统）决定推送策略
     *
     * @param message 消息对象
     */
    public void routeMessage(Message message) {
        if (message == null || message.getMsgType() == null) {
            log.warn("消息路由失败，消息为空或类型缺失");
            return;
        }

        switch (message.getMsgType()) {
            case 1:
                // 单聊
                pushToUser(message.getToUserId(), message);
                break;
            case 2:
                // 群聊
                pushToGroup(message.getGroupId(), message);
                break;
            case 3:
                // 系统消息
                pushToUser(message.getToUserId(), message);
                break;
            default:
                log.warn("未知消息类型: {}", message.getMsgType());
        }
    }

    /**
     * 向指定用户推送消息
     * 若用户在线则直接 WebSocket 推送，否则存入离线消息
     *
     * @param userId  用户ID
     * @param message 消息对象
     * @return true-推送成功或已存入离线 false-异常
     */
    public boolean pushToUser(Long userId, Message message) {
        if (userId == null || message == null) {
            return false;
        }

        String payload = JSON.toJSONString(message);

        if (channelManager.isOnline(userId)) {
            boolean sent = channelManager.sendToUser(userId, payload);
            if (sent) {
                log.info("消息推送成功, userId={}, messageId={}", userId, message.getId());
                return true;
            }
        }

        // 用户不在线或推送失败，存入离线消息
        offlineMessageService.storeOfflineMessage(message);
        log.info("用户[{}]不在线，消息存入离线, messageId={}", userId, message.getId());
        return true;
    }

    /**
     * 向群组广播消息
     * 遍历群成员列表，逐个推送或离线存储
     *
     * @param groupId 群组ID
     * @param message 消息对象
     */
    public void pushToGroup(Long groupId, Message message) {
        // TODO: 从群组服务获取群成员列表，此处简化处理
        log.info("群聊消息广播, groupId={}, messageId={}", groupId, message.getId());
        // 实际场景：获取群成员后循环调用 pushToUser
    }

    /**
     * 系统广播（全服推送）
     * 向所有在线用户推送系统消息
     *
     * @param message 系统消息内容
     */
    public void broadcast(Message message) {
        String payload = JSON.toJSONString(message);
        // TODO: 遍历所有在线用户进行推送，生产环境可结合 Redis Pub/Sub 实现集群广播
        log.info("系统广播消息: {}", payload);
    }

    /**
     * 系统广播（指定用户列表）
     *
     * @param userIds 目标用户ID集合
     * @param message 消息对象
     */
    public void broadcastToUsers(Collection<Long> userIds, Message message) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        for (Long userId : userIds) {
            pushToUser(userId, message);
        }
        log.info("批量推送完成, 目标用户数={}", userIds.size());
    }
}
