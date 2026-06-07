# TinyIM

TinyIM 是一款基于 Java 的高并发即时通讯（IM）系统，采用 Netty + RocketMQ + Redis + MySQL 技术栈，支持单聊、群聊、离线消息、消息顺序保证及多端登录互踢等核心能力。

## 技术架构

```
┌─────────────────────────────────────────────────────────────┐
│                        客户端 (Client)                       │
│                  WebSocket / 浏览器 / APP                    │
└───────────────────────────┬─────────────────────────────────┘
                            │
┌───────────────────────────▼─────────────────────────────────┐
│                     网关层 (Gateway)                         │
│  ┌──────────────┐  ┌──────────────────┐  ┌──────────────┐  │
│  │WebSocketServer│  │WebSocketHandler  │  │ChannelManager│  │
│  │  Netty 启动   │  │ 握手/心跳/帧处理  │  │ userId->Ch   │  │
│  └──────────────┘  └──────────────────┘  └──────────────┘  │
└───────────────────────────┬─────────────────────────────────┘
                            │
┌───────────────────────────▼─────────────────────────────────┐
│                   消息服务 (Message Service)                 │
│  ┌──────────────┐  ┌──────────────────┐  ┌──────────────┐  │
│  │MessageService│  │ MessageProducer  │  │MessageConsumer│  │
│  │ 单聊/群聊接口 │  │  RocketMQ 生产者  │  │RocketMQ 消费者 │  │
│  └──────────────┘  └──────────────────┘  └──────────────┘  │
│  ┌──────────────┐                                           │
│  │   WALLogger  │  写前日志，保证消息不丢失                    │
│  └──────────────┘                                           │
└───────────────────────────┬─────────────────────────────────┘
                            │
┌───────────────────────────▼─────────────────────────────────┐
│                   支撑服务 (Support Services)                │
│  ┌──────────────┐  ┌──────────────────┐  ┌──────────────┐  │
│  │MessageOrder  │  │OnlineStatus      │  │OfflineMessage│  │
│  │  顺序消息服务 │  │ Redis Bitmap     │  │  离线消息存储  │  │
│  └──────────────┘  └──────────────────┘  └──────────────┘  │
│  ┌──────────────┐  ┌──────────────────┐                    │
│  │ PushService  │  │UserSessionManager│  多端登录互踢      │
│  │ 推送/广播    │  │   会话生命周期    │                    │
│  └──────────────┘  └──────────────────┘                    │
└─────────────────────────────────────────────────────────────┘
```

## 核心模块说明

### 1. 网关层 (gateway/)

- **WebSocketServer**：基于 Netty 启动 WebSocket 服务，支持高并发长连接接入
- **WebSocketChannelInitializer**：配置 ChannelPipeline，集成 HTTP 编解码、WebSocket 握手、心跳检测
- **WebSocketHandler**：处理文本消息帧、Ping/Pong 心跳、Close 帧及读空闲超时检测
- **ChannelManager**：维护 `userId -> Channel` 映射，支持单用户单端在线（新登录踢掉旧连接）

### 2. 消息服务 (message/)

- **MessageService**：提供单聊/群聊发送接口，协调 WAL 落盘、MQ 投递、ACK 确认及重试逻辑
- **MessageProducer**：RocketMQ 生产者，支持普通消息与顺序消息（基于 hashKey 绑定单队列）
- **MessageConsumer**：RocketMQ 消费者，根据接收方在线状态决定推送客户端或存入离线消息
- **WALLogger**：写前日志（Write-Ahead Log），在消息正式落库/投递前追加到本地文件，用于故障恢复

### 3. 消息顺序 (order/)

- **MessageOrderService**：基于用户 ID 哈希分片绑定 RocketMQ 单队列，保证同一会话内消息严格有序
- 通过将两个用户 ID 按序拼接生成 `hashKey`，确保双向消息进入同一队列

### 4. 在线状态 (status/)

- **OnlineStatusService**：基于 Redis Bitmap 存储用户在线状态，千万级用户仅占用约 1.2 MB 内存
- **UserSessionManager**：处理用户上线/下线生命周期，触发多端互踢及离线消息推送事件

### 5. 离线消息 (offline/)

- **OfflineMessageService**：当接收方不在线时，将消息暂存为离线消息，待用户上线后批量推送
- **OfflineMessageMapper**：MyBatis Mapper，提供未推送消息查询、状态更新及历史数据清理能力

### 6. 推送服务 (push/)

- **PushService**：消息路由中心，支持单用户推送、群聊广播、系统全服广播及在线/离线自动决策

## 数据库表结构

详见 `src/main/resources/schema.sql`，包含以下核心表：

| 表名               | 说明           |
|--------------------|----------------|
| `user`             | 用户表         |
| `message`          | 消息主表       |
| `offline_message`  | 离线消息表     |

## 快速开始

### 环境依赖

- JDK 1.8+
- MySQL 8.0+
- Redis 6.0+
- RocketMQ 4.9+

### 初始化数据库

```bash
mysql -u root -p < src/main/resources/schema.sql
```

### 修改配置

编辑 `src/main/resources/application.properties`，配置数据库、Redis、RocketMQ 地址。

### 编译运行

```bash
mvn clean package
java -jar target/tinyim-1.0.0-SNAPSHOT.jar
```

### WebSocket 连接示例

```javascript
const ws = new WebSocket('ws://localhost:8081/ws');
ws.onopen = () => {
    ws.send(JSON.stringify({
        fromUserId: 10001,
        toUserId: 10002,
        msgType: 1,
        content: 'Hello, TinyIM!'
    }));
};
```

## 高并发设计要点

1. **Netty 事件驱动**：基于 NIO 实现百万级长连接，主从 Reactor 线程模型分离连接接入与 I/O 处理
2. **RocketMQ 削峰填谷**：消息异步投递，避免瞬时流量压垮数据库
3. **Redis Bitmap**：极低成本存储在线状态，BITCOUNT 毫秒级统计在线人数
4. **WAL 写前日志**：消息先写 WAL 再投递，保证故障场景下消息不丢失
5. **消息顺序保证**：用户 ID 哈希绑定单队列，避免乱序问题
6. **多端互踢**：ChannelManager 绑定新连接时自动关闭旧连接，保证单端在线

## 许可证

MIT License
