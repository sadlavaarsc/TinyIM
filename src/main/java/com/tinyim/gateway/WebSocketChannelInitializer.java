package com.tinyim.gateway;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * WebSocket Channel 初始化器
 * 配置 Netty ChannelPipeline，编解码器、心跳检测及业务处理器
 */
@Component
public class WebSocketChannelInitializer extends ChannelInitializer<SocketChannel> {

    @Value("${im.websocket.path:/ws}")
    private String websocketPath;

    @Value("${im.heartbeat.readerIdleTime:60}")
    private int readerIdleTime;

    @Value("${im.heartbeat.writerIdleTime:0}")
    private int writerIdleTime;

    @Value("${im.heartbeat.allIdleTime:0}")
    private int allIdleTime;

    @Autowired
    private WebSocketHandler webSocketHandler;

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ch.pipeline()
                // HTTP 编解码
                .addLast(new HttpServerCodec())
                // 支持大数据流
                .addLast(new ChunkedWriteHandler())
                // HTTP 消息聚合
                .addLast(new HttpObjectAggregator(65536))
                // WebSocket 握手处理
                .addLast(new WebSocketServerProtocolHandler(websocketPath, null, true, 65536))
                // 心跳检测：读空闲超过指定时间则触发 IdleStateEvent
                .addLast(new IdleStateHandler(readerIdleTime, writerIdleTime, allIdleTime, TimeUnit.SECONDS))
                // 业务处理器
                .addLast(webSocketHandler);
    }
}
