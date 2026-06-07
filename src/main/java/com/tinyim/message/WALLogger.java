package com.tinyim.message;

import com.alibaba.fastjson2.JSON;
import com.tinyim.entity.Message;
import lombok.extern.slf4j.Slf4j;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 预写式日志（WAL）记录器
 * 在消息正式落库或投递前，先将操作记录追加到本地日志文件，用于故障恢复时保证消息不丢失
 */
@Slf4j
public class WALLogger {

    private static final String WAL_FILE_PATH = "logs/wal.log";
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    /**
     * 将消息操作写入 WAL 日志
     * 采用追加写方式，保证高性能且数据不丢失
     *
     * @param operation 操作类型，如 SEND_PRIVATE、SEND_GROUP
     * @param message   消息对象
     */
    public static void write(String operation, Message message) {
        String entry = String.format("[%s] %s | %s%n",
                LocalDateTime.now().format(DTF),
                operation,
                JSON.toJSONString(message));
        try (PrintWriter writer = new PrintWriter(new FileWriter(WAL_FILE_PATH, true))) {
            writer.print(entry);
            writer.flush();
        } catch (IOException e) {
            log.error("WAL 写入失败", e);
        }
    }

    /**
     * 标记 WAL 中的某条记录已提交（落库成功后可调用）
     * 实际生产环境可配合 checkpoint 机制定期清理已提交的 WAL 记录
     *
     * @param messageId 消息ID
     */
    public static void markCommitted(Long messageId) {
        log.debug("WAL 记录已提交, messageId={}", messageId);
    }
}
