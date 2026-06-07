package com.tinyim.gateway;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * WebSocket 服务器
 * 基于 Netty 实现的高并发接入层，负责处理客户端 WebSocket 连接、握手及帧数据收发
 */
@Slf4j
@Component
public class WebSocketServer {

    @Value("${im.websocket.port:8088}")
    private int port;

    @Autowired
    private WebSocketChannelInitializer channelInitializer;

    /** 主线程组：接收客户端连接 */
    private EventLoopGroup bossGroup;

    /** 工作线程组：处理 I/O 读写 */
    private EventLoopGroup workerGroup;

    /**
     * 启动 WebSocket 服务器
     * 初始化 EventLoopGroup 并绑定端口，处理客户端连接
     */
    @PostConstruct
    public void start() {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();

        new Thread(() -> {
            try {
                ServerBootstrap bootstrap = new ServerBootstrap();
                bootstrap.group(bossGroup, workerGroup)
                        .channel(NioServerSocketChannel.class)
                        .option(ChannelOption.SO_BACKLOG, 1024)
                        .childOption(ChannelOption.SO_KEEPALIVE, true)
                        .childOption(ChannelOption.TCP_NODELAY, true)
                        .childHandler(channelInitializer);

                ChannelFuture future = bootstrap.bind(port).sync();
                log.info("WebSocket 服务器启动成功，监听端口: {}", port);
                future.channel().closeFuture().sync();
            } catch (InterruptedException e) {
                log.error("WebSocket 服务器异常", e);
                Thread.currentThread().interrupt();
            } finally {
                shutdown();
            }
        }, "WebSocket-Server-Thread").start();
    }

    /**
     * 优雅关闭 WebSocket 服务器
     * 释放 EventLoopGroup 资源
     */
    @PreDestroy
    public void shutdown() {
        log.info("WebSocket 服务器正在关闭...");
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        log.info("WebSocket 服务器已关闭");
    }
}
