package com.tinyim.status;

import com.tinyim.gateway.ChannelManager;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 用户会话管理器
 * 处理用户上线、下线事件，维护会话生命周期，触发多端互踢及状态变更事件
 */
@Slf4j
@Component
public class UserSessionManager {

    @Autowired
    private ChannelManager channelManager;

    @Autowired
    private OnlineStatusService onlineStatusService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    /**
     * 用户上线处理
     * 更新在线状态、绑定 Channel，并触发上线事件
     *
     * @param userId  用户ID
     * @param channel Netty Channel
     */
    public void userOnline(Long userId, Channel channel) {
        // 绑定用户与 Channel（内部已实现多端互踢）
        channelManager.bind(userId, channel);

        // 更新 Redis Bitmap 在线状态
        onlineStatusService.setOnline(userId, true);

        // 发布上线事件（用于离线消息推送等）
        eventPublisher.publishEvent(new UserOnlineEvent(this, userId));

        log.info("用户[{}]上线处理完成", userId);
    }

    /**
     * 用户下线处理
     * 清理 Channel 绑定、更新在线状态，并触发下线事件
     *
     * @param userId 用户ID
     */
    public void userOffline(Long userId) {
        // 更新 Redis Bitmap 为离线
        onlineStatusService.setOnline(userId, false);

        // 发布下线事件
        eventPublisher.publishEvent(new UserOfflineEvent(this, userId));

        log.info("用户[{}]下线处理完成", userId);
    }

    /**
     * 用户上线事件
     */
    public static class UserOnlineEvent extends org.springframework.context.ApplicationEvent {
        private final Long userId;

        public UserOnlineEvent(Object source, Long userId) {
            super(source);
            this.userId = userId;
        }

        public Long getUserId() {
            return userId;
        }
    }

    /**
     * 用户下线事件
     */
    public static class UserOfflineEvent extends org.springframework.context.ApplicationEvent {
        private final Long userId;

        public UserOfflineEvent(Object source, Long userId) {
            super(source);
            this.userId = userId;
        }

        public Long getUserId() {
            return userId;
        }
    }
}
