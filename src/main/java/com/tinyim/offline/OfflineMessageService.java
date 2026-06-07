package com.tinyim.offline;

import com.alibaba.fastjson2.JSON;
import com.tinyim.entity.Message;
import com.tinyim.entity.OfflineMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 离线消息服务
 * 当接收方不在线时，将消息暂存为离线消息，待用户上线后批量推送
 */
@Slf4j
@Service
public class OfflineMessageService {

    @Autowired
    private OfflineMessageMapper offlineMessageMapper;

    /**
     * 存储离线消息
     * 将消息转换为离线消息实体并写入数据库
     *
     * @param message 原始消息对象
     */
    public void storeOfflineMessage(Message message) {
        OfflineMessage offlineMessage = new OfflineMessage();
        offlineMessage.setUserId(message.getToUserId());
        offlineMessage.setFromUserId(message.getFromUserId());
        offlineMessage.setContent(JSON.toJSONString(message));
        offlineMessage.setMsgType(message.getMsgType());
        offlineMessage.setSequence(message.getSequence());
        offlineMessage.setPushed(0);
        offlineMessage.setCreateTime(LocalDateTime.now());

        offlineMessageMapper.insert(offlineMessage);
        log.info("离线消息已存储, userId={}, fromUserId={}, messageId={}",
                message.getToUserId(), message.getFromUserId(), message.getId());
    }

    /**
     * 获取用户的未推送离线消息
     *
     * @param userId 用户ID
     * @return 离线消息列表
     */
    public List<OfflineMessage> getUnpushedMessages(Long userId) {
        return offlineMessageMapper.selectUnpushedByUserId(userId);
    }

    /**
     * 标记离线消息为已推送
     *
     * @param id 离线消息ID
     */
    public void markAsPushed(Long id) {
        offlineMessageMapper.updatePushedStatus(id, 1, LocalDateTime.now());
        log.info("离线消息标记为已推送, id={}", id);
    }

    /**
     * 批量标记已推送
     *
     * @param ids 离线消息ID列表
     */
    public void batchMarkAsPushed(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        offlineMessageMapper.batchUpdatePushedStatus(ids, 1, LocalDateTime.now());
        log.info("批量标记离线消息已推送, 数量={}", ids.size());
    }

    /**
     * 清理已推送的历史离线消息
     * 可由定时任务调用，防止数据无限增长
     *
     * @param days 保留天数
     */
    public void cleanOldPushedMessages(int days) {
        LocalDateTime beforeTime = LocalDateTime.now().minusDays(days);
        int count = offlineMessageMapper.deletePushedBefore(beforeTime);
        log.info("清理离线消息完成, 删除数量={}, 保留天数={}", count, days);
    }
}
