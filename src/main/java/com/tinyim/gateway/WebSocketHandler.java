package com.tinyim.gateway;

import com.alibaba.fastjson2.JSON;
import com.tinyim.entity.Message;
import com.tinyim.push.PushService;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * WebSocket 业务处理器
 * 处理 WebSocket 握手完成后的各类事件：文本消息、心跳、连接断开及空闲检测
 */
@Slf4j
@Component
@ChannelHandler.Sharable
public class WebSocketHandler extends SimpleChannelInboundHandler<WebSocketFrame> {

    @Autowired
    private ChannelManager channelManager;

    @Autowired
    private PushService pushService;

    /**
     * 处理 WebSocket 帧数据
     * 支持文本消息、关闭帧、Ping/Pong 帧
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) throws Exception {
        Channel channel = ctx.channel();

        if (frame instanceof TextWebSocketFrame) {
            String text = ((TextWebSocketFrame) frame).text();
            log.debug("收到文本消息: {}", text);
            handleTextMessage(channel, text);
        } else if (frame instanceof CloseWebSocketFrame) {
            log.info("收到关闭帧，Channel: {}", channel.id());
            channel.close();
        } else if (frame instanceof PingWebSocketFrame) {
            // 自动回复 Pong
            channel.writeAndFlush(new PongWebSocketFrame(frame.content().retain()));
        } else if (frame instanceof PongWebSocketFrame) {
            log.debug("收到 Pong 帧，Channel: {}", channel.id());
        } else {
            log.warn("不支持的 WebSocket 帧类型: {}", frame.getClass().getName());
        }
    }

    /**
     * 处理文本消息
     * 解析 JSON 格式消息，根据消息类型进行分发
     *
     * @param channel Netty Channel
     * @param text    文本内容
     */
    private void handleTextMessage(Channel channel, String text) {
        try {
            Message message = JSON.parseObject(text, Message.class);
            // 若消息中包含用户ID，则进行绑定（简化版登录鉴权）
            if (message.getFromUserId() != null) {
                channelManager.bind(message.getFromUserId(), channel);
            }
            // 消息投递到消息服务处理
            pushService.routeMessage(message);
        } catch (Exception e) {
            log.error("消息解析失败: {}", text, e);
        }
    }

    /**
     * 连接建立时触发
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("客户端连接建立: {}", ctx.channel().id());
        super.channelActive(ctx);
    }

    /**
     * 连接断开时触发
     * 清理用户 Channel 绑定关系
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        log.info("客户端连接断开: {}", channel.id());
        channelManager.unbind(channel);
        super.channelInactive(ctx);
    }

    /**
     * 用户事件触发
     * 处理心跳读空闲事件，自动剔除死连接
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state() == IdleState.READER_IDLE) {
                log.warn("读空闲超时，关闭连接: {}", ctx.channel().id());
                ctx.channel().close();
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    /**
     * 异常捕获
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("通道异常: {}", ctx.channel().id(), cause);
        ctx.channel().close();
    }
}
