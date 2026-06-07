package com.tinyim.status;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 在线状态服务
 * 基于 Redis Bitmap 存储用户在线状态，支持高并发下的状态查询与更新
 * Bitmap 优势：每个用户仅占用 1 bit，千万级用户仅需约 1.2 MB 内存
 */
@Slf4j
@Service
public class OnlineStatusService {

    private static final String ONLINE_BITMAP_KEY = "im:online:bitmap";

    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     * 设置用户在线状态
     *
     * @param userId 用户ID
     * @param online true-在线 false-离线
     */
    public void setOnline(Long userId, boolean online) {
        redisTemplate.opsForValue().setBit(ONLINE_BITMAP_KEY, userId, online);
        log.info("用户[{}]在线状态设置为: {}", userId, online);
    }

    /**
     * 查询用户是否在线
     *
     * @param userId 用户ID
     * @return true-在线 false-离线
     */
    public boolean isOnline(Long userId) {
        Boolean result = redisTemplate.opsForValue().getBit(ONLINE_BITMAP_KEY, userId);
        return result != null && result;
    }

    /**
     * 统计在线用户数量
     * 使用 Redis BITCOUNT 命令统计 Bitmap 中值为 1 的位数
     *
     * @return 在线用户数
     */
    public Long countOnline() {
        Long count = redisTemplate.execute(connection -> connection.bitCount(ONLINE_BITMAP_KEY.getBytes()), false);
        return count != null ? count : 0L;
    }

    /**
     * 批量设置用户在线状态（用于批量上线/下线场景）
     *
     * @param userIds 用户ID数组
     * @param online  在线状态
     */
    public void batchSetOnline(Long[] userIds, boolean online) {
        for (Long userId : userIds) {
            redisTemplate.opsForValue().setBit(ONLINE_BITMAP_KEY, userId, online);
        }
        log.info("批量设置在线状态, 数量={}, online={}", userIds.length, online);
    }
}
