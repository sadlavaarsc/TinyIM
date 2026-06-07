package com.tinyim.demo;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.*;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.util.Scanner;

/**
 * 简易 WebSocket 客户端示例
 * 用于演示连接 TinyIM 网关、发送消息及接收服务端推送
 */
@Slf4j
public class ClientDemo {

    private static final String SERVER_URL = "ws://127.0.0.1:8088/ws";
    private Channel channel;

    public static void main(String[] args) throws Exception {
        ClientDemo client = new ClientDemo();
        client.connect();

        Scanner scanner = new Scanner(System.in);
        System.out.println("请输入消息内容（格式: {\"fromUserId\":1,\"toUserId\":2,\"content\":\"hello\"}），输入 exit 退出：");
        while (true) {
            String line = scanner.nextLine();
            if ("exit".equalsIgnoreCase(line)) {
                client.close();
                break;
            }
            client.send(line);
        }
        scanner.close();
    }

    /**
     * 建立 WebSocket 连接
     */
    public void connect() throws Exception {
        URI uri = new URI(SERVER_URL);
        EventLoopGroup group = new NioEventLoopGroup();

        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline()
                                .addLast(new HttpClientCodec())
                                .addLast(new HttpObjectAggregator(65536))
                                .addLast(new WebSocketClientHandler());
                    }
                });

        channel = bootstrap.connect(uri.getHost(), uri.getPort()).sync().channel();

        WebSocketClientHandshaker handshaker = WebSocketClientHandshakerFactory.newHandshaker(
                uri, WebSocketVersion.V13, null, true, new DefaultHttpHeaders());
        WebSocketClientHandler handler = channel.pipeline().get(WebSocketClientHandler.class);
        handler.setHandshaker(handshaker);
        handshaker.handshake(channel).sync();
        log.info("WebSocket 连接成功: {}", SERVER_URL);
    }

    /**
     * 发送文本消息
     *
     * @param text 消息内容
     */
    public void send(String text) {
        if (channel != null && channel.isActive()) {
            channel.writeAndFlush(new TextWebSocketFrame(text));
            log.info("客户端发送: {}", text);
        } else {
            log.warn("连接未建立，无法发送消息");
        }
    }

    /**
     * 关闭连接
     */
    public void close() {
        if (channel != null) {
            channel.close();
        }
        log.info("客户端连接已关闭");
    }

    /**
     * WebSocket 客户端处理器
     */
    @ChannelHandler.Sharable
    static class WebSocketClientHandler extends SimpleChannelInboundHandler<WebSocketFrame> {

        private WebSocketClientHandshaker handshaker;

        public void setHandshaker(WebSocketClientHandshaker handshaker) {
            this.handshaker = handshaker;
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) {
            if (frame instanceof TextWebSocketFrame) {
                String text = ((TextWebSocketFrame) frame).text();
                System.out.println("收到服务端消息: " + text);
            } else if (frame instanceof PongWebSocketFrame) {
                log.debug("收到 Pong");
            } else if (frame instanceof CloseWebSocketFrame) {
                log.info("收到关闭帧");
                ctx.channel().close();
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            log.error("客户端异常", cause);
            ctx.close();
        }
    }
}
