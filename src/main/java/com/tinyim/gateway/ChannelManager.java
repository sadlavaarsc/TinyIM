package com.tinyim.gateway;

import io.netty.channel.Channel;
import io.netty.channel.ChannelId;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 用户 Channel 管理器
 * 负责维护用户ID与Netty Channel之间的映射关系，支持多端登录互踢
 */
@Slf4j
@Component
public class ChannelManager {

    /** Channel中存储用户ID的AttributeKey */
    public static final AttributeKey<Long> USER_ID_KEY = AttributeKey.valueOf("userId");

    /** 用户ID -> Channel 映射（支持单端在线，新登录踢掉旧连接） */
    private final ConcurrentHashMap<Long, Channel> userChannelMap = new ConcurrentHashMap<>();

    /** ChannelId -> 用户ID 映射（用于连接断开时快速查找） */
    private final ConcurrentHashMap<ChannelId, Long> channelUserMap = new ConcurrentHashMap<>();

    /**
     * 绑定用户ID与Channel
     * 若该用户已有其他连接，则关闭旧连接（多端互踢）
     *
     * @param userId  用户ID
     * @param channel Netty Channel
     */
    public void bind(Long userId, Channel channel) {
        Channel oldChannel = userChannelMap.put(userId, channel);
        if (oldChannel != null && oldChannel.isActive() && !oldChannel.id().equals(channel.id())) {
            log.info("用户[{}]在新端登录，关闭旧连接: {}", userId, oldChannel.id());
            oldChannel.close();
        }
        channelUserMap.put(channel.id(), userId);
        channel.attr(USER_ID_KEY).set(userId);
        log.info("用户[{}]绑定Channel成功: {}", userId, channel.id());
    }

    /**
     * 解绑Channel
     * 当连接断开时调用，清理映射关系
     *
     * @param channel Netty Channel
     */
    public void unbind(Channel channel) {
        Long userId = channelUserMap.remove(channel.id());
        if (userId != null) {
            userChannelMap.remove(userId, channel);
            channel.attr(USER_ID_KEY).set(null);
            log.info("用户[{}]解绑Channel: {}", userId, channel.id());
        }
    }

    /**
     * 根据用户ID获取Channel
     *
     * @param userId 用户ID
     * @return 用户对应的Channel，若不在线则返回null
     */
    public Channel getChannel(Long userId) {
        return userChannelMap.get(userId);
    }

    /**
     * 判断用户是否在线
     *
     * @param userId 用户ID
     * @return true-在线 false-离线
     */
    public boolean isOnline(Long userId) {
        Channel channel = userChannelMap.get(userId);
        return channel != null && channel.isActive();
    }

    /**
     * 获取在线用户数量
     *
     * @return 在线用户数
     */
    public int onlineCount() {
        return userChannelMap.size();
    }
}
