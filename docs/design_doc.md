# 基于图形界面的 C/S 结构简易聊天室 — 设计文档

## 一、设计思路

### 1.1 整体架构

系统采用经典的 **C/S（客户端/服务器）架构**：

- **服务端**集中管理用户认证、消息路由、在线状态和日志记录，是系统的中枢
- **客户端**通过 Swing GUI 提供登录和聊天界面，只负责展示和发送，不直接与其他客户端通信

选择 C/S 而非 P2P 的理由：
- 用户认证需要一个可信的中央节点来校验密码
- 消息转发由服务端统一处理，能保证匿名聊天真正隐藏发送者身份（服务端替换 `from` 字段）
- 在线用户列表、日志记录等功能天然适合集中管理

### 1.2 分层分包

代码分为三个包，遵循"公共层 + 服务端 + 客户端"的分层策略：

```
src/
├── common/    ← 共享协议层（Constants, Message, MessageType）
├── server/    ← 服务端逻辑（ChatServer, ClientHandler, UserManager, LogManager, ServerConsole）
└── client/    ← 客户端逻辑（ChatClient, LoginFrame, ChatFrame, ClientReceiver）
```

- `common` 包不依赖 `server` 或 `client`，可独立编译复用
- `server` 和 `client` 都依赖 `common`，但它们之间没有代码级耦合，仅通过 Socket 通信

### 1.3 消息协议设计

自定义文本协议，格式为管道分隔的五个字段：

```
TYPE|from|to|content|timestamp
```

**设计考量：**

| 决策 | 原因 |
|------|------|
| 文本协议而非二进制 | 易于调试（可用 `nc` 手动测试）、阅读，满足课程要求 |
| 管道符 `\|` 作为分隔符 | 用户输入极少包含管道符；协议中 `from`/`to` 不包含 `|`，`timestamp` 是纯数字，只有 `content` 可能包含 `|` |
| `content` 可为空字符串 | 登录、命令类消息不需要正文 |
| `timestamp` 放在末尾 | 配合 `lastIndexOf('|')`，可以安全地解析可能包含 `|` 的 `content` |
| 10 种消息类型 | `MessageType` 枚举覆盖登录验证、聊天、系统通知、错误四大类场景 |

**解码策略：** `decode()` 方法先用 `lastIndexOf('|')` 定位时间戳（保证不受 `content` 中的 `|` 干扰），再用 `split("\\|", 4)` 解析前四段，实现了对管道符出现在消息正文中的鲁棒处理。

### 1.4 多线程模型

```
服务端线程模型：
  main 线程         → 启动 ServerSocket，循环 accept()
  ServerConsole 线程 → 读取管理员命令（list / listall / quit）
  ClientHandler 线程  → 每个客户端一个线程，处理该客户端的全部消息

客户端线程模型：
  main / EDT 线程    → GUI 事件处理和界面更新（Swing 单线程规则）
  ClientReceiver 线程 → 后台阻塞读取服务器消息，通过 SwingUtilities.invokeLater 分发到 EDT
```

**为什么每个客户端一个线程？**
- `BufferedReader.readLine()` 是阻塞调用，必须放在独立线程中
- 每个线程内部是简单的"读-处理"循环，逻辑清晰，便于维护
- 线程数上限等于同时在线用户数（课程要求 ≥5，实际可达数十个），在可接受范围

**客户端为何额外用 EDT？**
- Swing 要求所有界面更新必须在 Event Dispatch Thread 上执行
- `ClientReceiver` 收到的消息通过 `SwingUtilities.invokeLater()` 安全地投递到 EDT

### 1.5 线程安全设计

| 场景 | 手段 |
|------|------|
| 在线用户集合并发读写 | `ConcurrentHashMap.newKeySet()` |
| 已注册用户 / 在线用户 Map | `ConcurrentHashMap`（`putIfAbsent` 防重复登录） |
| 日志写入 | `LogManager` 所有写方法 `synchronized` |
| 客户端退出防重入 | `ClientHandler.cleanup()` `synchronized` + `cleanedUp` 布尔标志 |
| 服务端运行状态 | `volatile boolean running` |
| GUI 关闭窗口防重入 | `ChatFrame.closing` volatile 标志 |

### 1.6 GUI 设计

- 登录界面 ([LoginFrame](src/client/LoginFrame.java))：`GridBagLayout` 居中排列，简洁直观
- 聊天界面 ([ChatFrame](src/client/ChatFrame.java))：`BorderLayout` 五区域布局
  - NORTH：用户信息 + 切换匿名按钮
  - CENTER：消息显示区（`JTextArea`，只读，自动换行）
  - EAST：在线用户列表（`JList`）
  - SOUTH：输入提示 + 输入框 + 发送按钮

界面流程：`LoginFrame` →（登录成功）→ `ChatFrame` →（退出/关闭）→ 程序结束

---

## 二、每个类的功能

### 2.1 common 包（公共层）

| 类 | 文件 | 功能 |
|----|------|------|
| `Constants` | [Constants.java](src/common/Constants.java) | 集中管理所有配置常量：服务器地址、端口、文件路径、字符集、协议前缀和分隔符 |
| `MessageType` | [MessageType.java](src/common/MessageType.java) | 枚举 10 种消息类型，覆盖登录、聊天、系统通知、错误四大类 |
| `Message` | [Message.java](src/common/Message.java) | 消息实体 + 编解码核心类。`encode()` 序列化为协议字符串，`decode()` 反序列化并鲁棒处理 `content` 中含管道符的边界情况 |

### 2.2 server 包（服务端）

| 类 | 文件 | 功能 |
|----|------|------|
| `ChatServer` | [ChatServer.java](src/server/ChatServer.java) | 服务器主类（`main` 入口）。启动流程：加载用户文件 → 启动控制台线程 → 绑定端口 → 循环 `accept()` 为每个客户端创建 `ClientHandler` 线程。提供 `broadcast()`、`broadcastExcept()`、`sendToUser()` 三种消息分发策略 |
| `ClientHandler` | [ClientHandler.java](src/server/ClientHandler.java) | 客户端连接处理器（`Runnable`）。分为两个阶段：**登录阶段**仅处理 `LOGIN` 消息，反复验证直到成功或断开；**聊天阶段**处理 `BROADCAST` / `PRIVATE` / `SYSTEM` 三类消息。匿名模式下将 `from` 替换为 `"anonymous"`。`cleanup()` 加同步锁防重复调用 |
| `ServerConsole` | [ServerConsole.java](src/server/ServerConsole.java) | 服务端管理员控制台（`Runnable`）。从 `System.in` 读取命令：`list`（在线用户）、`listall`（全部注册用户）、`quit`（优雅关闭服务器） |
| `UserManager` | [UserManager.java](src/server/UserManager.java) | 用户管理。两个 `ConcurrentHashMap` 分别存储注册用户（name→password）和在线用户（name→ClientHandler）。`addOnlineUser()` 用 `putIfAbsent` 原子操作防重复登录。列表方法返回排序结果 |
| `LogManager` | [LogManager.java](src/server/LogManager.java) | 日志管理器。每日滚动日志（`logs/chat_yyyy-MM-dd.log`），记录时间、操作类型、用户名、IP 和详情。所有写方法 `synchronized` 保证多线程写入安全 |

### 2.3 client 包（客户端）

| 类 | 文件 | 功能 |
|----|------|------|
| `ChatClient` | [ChatClient.java](src/client/ChatClient.java) | 客户端网络层。封装 Socket 连接、消息发送/接收。`login()` 方法同步等待服务器响应。提供 `sendBroadcast()`、`sendPrivate()`、`sendCommand()` 三个便捷发送方法 |
| `LoginFrame` | [LoginFrame.java](src/client/LoginFrame.java) | 登录窗口（客户端 `main` 入口）。启动即连接服务器，连接失败弹窗退出。登录操作在后台线程执行，不阻塞 UI。成功跳转 `ChatFrame`，失败显示原因并保留登录窗口 |
| `ChatFrame` | [ChatFrame.java](src/client/ChatFrame.java) | 聊天室主界面。`BorderLayout` 布局包含消息区、在线列表、输入区。输入解析：无前缀→群聊、`@`→私聊、`@@`→命令。`onMessage()` 根据消息类型分派显示逻辑。`closeWindow()` 带 `volatile closing` 防重入 |
| `ClientReceiver` | [ClientReceiver.java](src/client/ClientReceiver.java) | 后台接收线程（`Runnable`）。循环 `readMessage()` 阻塞读取，通过 `SwingUtilities.invokeLater()` 将消息安全投递到 EDT。连接断开时通知 `ChatFrame` 并触发关闭 |

---

## 三、类的关系

### 3.1 整体关系图（文字描述）

```
                        ┌──────────────────────┐
                        │      common 包        │
                        │  Constants            │
                        │  Message (编解码)      │
                        │  MessageType (枚举)   │
                        └────┬─────┬───────────┘
                             │     │
                   ┌─────────┘     └─────────┐
                   ▼ 依赖                     ▼ 依赖
        ┌──────────────────┐       ┌──────────────────┐
        │    server 包      │       │    client 包      │
        └──────────────────┘       └──────────────────┘
```

### 3.2 服务端类关系

```
ChatServer (main 入口，系统中枢)
    │
    ├── 组合 ──► UserManager     (注册用户 + 在线用户管理)
    ├── 组合 ──► LogManager      (日志写入)
    ├── 创建并启动 ──► ServerConsole  (Runnable，独立线程)
    └── 创建并启动 ──► ClientHandler  (Runnable，每个客户端一个线程)
                           │
                           ├── 依赖 ──► ChatServer    (回调 broadcast / sendToUser / getUserManager / getLogManager)
                           ├── 依赖 ──► UserManager   (认证、上下线管理)
                           └── 依赖 ──► LogManager    (记录登录/退出/失败)
```

**关系说明：**
- `ChatServer` **组合** `UserManager` 和 `LogManager`：它们的生命周期与服务器一致，服务器销毁时一并释放
- `ChatServer` **创建** `ClientHandler` 和 `ServerConsole` 线程：一对多关系，每个客户端连接产生一个 `ClientHandler`
- `ClientHandler` **依赖** `ChatServer`（通过引用回调），进而间接使用 `UserManager` 和 `LogManager`
- `ServerConsole` **依赖** `ChatServer`：调用 `shutdown()` 触发优雅关闭

### 3.3 客户端类关系

```
LoginFrame (main 入口)
    │
    ├── 创建并持有 ──► ChatClient  (网络通信，生命周期贯穿登录到退出)
    │
    └── 登录成功时创建 ──► ChatFrame
                              │
                              ├── 接收 ChatClient 引用 ──► 同一個 ChatClient 实例
                              └── 创建并启动 ──► ClientReceiver (Runnable)
                                                     │
                                                     ├── 依赖 ──► ChatClient  (调用 readMessage)
                                                     └── 依赖 ──► ChatFrame   (调用 onMessage 更新界面)
```

**关系说明：**
- `LoginFrame` **创建** `ChatClient`，登录成功后传递给 `ChatFrame`——同一个 `ChatClient` 实例在登录和聊天阶段复用，避免重复连接
- `ChatFrame` **创建** `ClientReceiver` 线程处理后台接收
- `ClientReceiver` **依赖** `ChatClient`（读取消息）和 `ChatFrame`（投递消息到界面）
- `ChatFrame` 通过 `closeWindow()` 统一负责资源清理：停止 `ClientReceiver` → 关闭 `ChatClient` → `dispose()` 窗口

### 3.4 消息流图

```
                                 ChatServer
                                    │
                    ┌───────────────┼───────────────┐
                    │               │               │
              broadcast()    sendToUser()    broadcastExcept()
                    │               │               │
                    ▼               ▼               ▼
            ┌───────────────────────────────────────────┐
            │         ClientHandler (×N)                 │
            │   ┌─ handleBroadcast ─► 替换 from (匿名)    │
            │   ├─ handlePrivate   ─► 替换 from (匿名)    │
            │   └─ handleSystem    ─► 命令分发            │
            └───────────────────────────────────────────┘
                    │ sendMessage(msg.encode())
                    ▼
            ┌───────────────────┐
            │   TCP Socket 网络   │
            └───────────────────┘
                    │ readMessage() → Message.decode()
                    ▼
            ┌───────────────────┐
            │   ClientReceiver   │
            │   (后台接收线程)    │
            └──────┬────────────┘
                   │ SwingUtilities.invokeLater
                   ▼
            ┌───────────────────┐
            │    ChatFrame       │
            │  onMessage(msg)    │
            │  更新消息区/在线列表 │
            └───────────────────┘
```

---

## 四、使用说明

### 4.1 编译

```bash
cd /data/xjr/JavaLab
javac -encoding UTF-8 -d out src/common/*.java src/server/*.java src/client/*.java
```

编译产物输出到 `out/` 目录，按包结构组织。

### 4.2 启动服务器

```bash
java -cp out server.ChatServer
```

启动后输出：
```
[INFO] loaded 12 registered users
[INFO] server started on port 8888
[CONSOLE] commands: list | listall | quit
```

服务器控制台命令：

| 命令 | 作用 |
|------|------|
| `list` | 列出当前在线用户及数量 |
| `listall` | 列出全部注册用户 |
| `quit` | 优雅关闭服务器（通知所有客户端后断开） |

### 4.3 启动客户端

打开一个或多个终端（至少可开 5 个），每个执行：

```bash
java -cp out client.LoginFrame
```

### 4.4 登录

- 在弹出的登录窗口输入用户名和密码（见下方测试账号）
- 点击"登录"或按回车
- 失败：显示错误原因，密码框清空，可重新输入
- 成功：进入聊天室主界面

### 4.5 聊天操作

在底部输入框输入内容后按回车或点击"发送"：

| 输入格式 | 示例 | 效果 |
|----------|------|------|
| 普通文本 | `大家好！` | 群聊，所有在线用户可见，显示为 `[群聊] 用户名: 内容` |
| `@用户名 内容` | `@lisi 你好` | 私聊，仅目标用户可见，显示为 `[私聊] 用户名 → 我: 内容` |
| `@@list` | — | 刷新在线用户列表 |
| `@@showanonymous` | — | 查看当前是否处于匿名模式 |
| `@@anonymous` | — | 切换匿名/实名模式（切换后消息发送者显示为 `anonymous`） |
| `@@quit` | — | 退出聊天室 |

另有顶部"切换匿名"按钮，功能等同于 `@@anonymous`。

### 4.6 日志

服务器自动在 `logs/` 目录生成每日日志文件（`chat_yyyy-MM-dd.log`），记录内容：

- `LOGIN_SUCCESS`：用户名、IP、登录时间
- `LOGIN_FAIL`：尝试的用户名、IP、失败时间
- `LOGOUT`：用户名、IP、退出时间
- `SYSTEM`：服务器启停事件

### 4.7 测试账号

| 用户名 | 密码 |
|--------|------|
| zhangsan | 123456 |
| lisi | 123456 |
| wangwu | 123456 |
| zhaoliu | 123456 |
| sunqi | 123456 |
| zhouba | 123456 |
| wujiu | 123456 |
| zhengshi | password |
| qianyi | password |
| liuer | password |
| testuser | test123 |
| admin | admin123 |

### 4.8 示例：同时启动 5 个客户端

终端 1：启动服务器
```bash
java -cp out server.ChatServer
```

终端 2~6：各启动一个客户端
```bash
java -cp out client.LoginFrame
```

分别以 zhangsan、lisi、wangwu、zhaoliu、sunqi 登录，即可测试群聊、私聊、匿名切换、用户上下线通知等全部功能。详细验收步骤参见 [gui_acceptance_test.md](gui_acceptance_test.md)。
