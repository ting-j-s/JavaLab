# 最终文件清单

## 项目结构

```
/data/xjr/JavaLab/
├── src/
│   ├── common/          # 公共类（服务端和客户端共享）
│   │   ├── Constants.java      # 配置常量
│   │   ├── Message.java        # 消息编解码
│   │   └── MessageType.java    # 消息类型枚举
│   ├── server/          # 服务端
│   │   ├── ChatServer.java     # 服务器主类
│   │   ├── ClientHandler.java  # 客户端连接处理
│   │   ├── ServerConsole.java  # 服务器控制台
│   │   ├── UserManager.java    # 用户管理
│   │   └── LogManager.java     # 日志管理
│   └── client/          # 客户端
│       ├── LoginFrame.java     # 登录窗口（含 main 入口）
│       ├── ChatFrame.java      # 聊天主界面
│       ├── ChatClient.java     # 客户端网络层
│       └── ClientReceiver.java # 后台接收线程
├── resources/
│   └── users.txt               # 用户账号文件（12 个用户）
├── logs/                       # 日志目录（自动创建）
│   └── chat_yyyy-MM-dd.log    # 每日滚动日志
├── docs/
│   ├── gui_acceptance_test.md  # GUI 手动验收文档
│   └── final_file_list.md      # 本文件
├── TestMessage.java            # Message 编解码单元测试
├── TestServerCore.java         # 服务端核心逻辑测试
├── TestServerIntegration.java  # 服务端集成测试（27 项）
├── TestServerManual.md         # 服务端手动测试说明
├── ClientManualTest.md         # 客户端手动测试说明
└── out/                        # 编译输出目录
```

## 1. common 包

### [Constants.java](src/common/Constants.java)
- 集中管理所有配置常量
- `SERVER_HOST`: 服务器地址 127.0.0.1
- `SERVER_PORT`: 服务器端口 8888（> 8000）
- `USER_FILE_PATH`: 用户文件路径
- `LOG_DIR`: 日志目录
- `CHARSET`: 字符集 UTF-8
- `PRIVATE_PREFIX`: 私聊前缀 `@`
- `COMMAND_PREFIX`: 命令前缀 `@@`

### [Message.java](src/common/Message.java)
- 消息编解码核心类
- 协议格式: `TYPE|from|to|content|timestamp`
- `encode()`: 序列化为管道分隔字符串
- `decode(String)`: 从字符串反序列化，支持 content 中包含 `|`（使用 `lastIndexOf('|')` 解析时间戳）
- 字段: type, from, to, content, timestamp

### [MessageType.java](src/common/MessageType.java)
- 消息类型枚举（10 个值）
- LOGIN, LOGIN_SUCCESS, LOGIN_FAIL, BROADCAST, PRIVATE, SYSTEM, USER_LIST, USER_JOIN, USER_LEAVE, ERROR

## 2. server 包

### [ChatServer.java](src/server/ChatServer.java)
- 服务器主类，main 入口
- 启动流程: 加载用户文件 → 启动控制台线程 → 监听端口 → 接受客户端连接
- `broadcast()`: 向所有已登录客户端广播
- `broadcastExcept()`: 向除指定客户端外的所有客户端广播（用于 USER_JOIN/USER_LEAVE）
- `sendToUser()`: 发送私聊消息给指定用户
- `shutdown()`: 优雅关闭，通知所有客户端并关闭 ServerSocket

### [ClientHandler.java](src/server/ClientHandler.java)
- 每个客户端一个实例，实现 Runnable
- 分为登录阶段（仅接受 LOGIN）和聊天阶段（BROADCAST/PRIVATE/SYSTEM）
- 登录: 校验用户名密码 → 防重复登录 → 发送 LOGIN_SUCCESS / LOGIN_FAIL
- 群聊: 转发时根据 anonymousMode 替换 from 为 "anonymous"
- 私聊: 发送给目标用户，确认给发送者
- 匿名: toggle anonymousMode 布尔值
- cleanup: synchronized + cleanedUp 标志防重复，发送 USER_LEAVE 并清理

### [ServerConsole.java](src/server/ServerConsole.java)
- 服务器管理员控制台线程
- 命令: `list`（在线用户）, `listall`（全部注册用户）, `quit`（关闭服务器）
- 从 System.in 读取，try-catch 处理 NoSuchElementException

### [UserManager.java](src/server/UserManager.java)
- 用户管理: 已注册用户（ConcurrentHashMap）+ 在线用户（ConcurrentHashMap）
- `loadUsersFromFile()`: 从 resources/users.txt 加载，格式 `用户名 密码`，支持 # 注释和空行
- `authenticate()`: 验证用户名密码
- `addOnlineUser()`: putIfAbsent 防重入
- `removeOnlineUser()`: 移除在线用户
- 列表方法返回排序后的 List（TreeSet）

### [LogManager.java](src/server/LogManager.java)
- 每日滚动日志（logs/chat_yyyy-MM-dd.log）
- 格式: `[yyyy-MM-dd HH:mm:ss] ACTION | user=username | ip=ip | detail`
- 所有方法 synchronized 线程安全
- 自动创建 logs 目录
- 记录: LOGIN_SUCCESS, LOGIN_FAIL, LOGOUT, system 事件

## 3. client 包

### [LoginFrame.java](src/client/LoginFrame.java)
- 客户端启动入口（含 main 方法）
- 启动流程: 创建 ChatClient → 连接服务器 → 显示登录界面
- 连接失败弹窗提示并退出
- 登录在后台线程执行，避免阻塞 Swing UI
- 成功: 跳转 ChatFrame；失败: 显示错误原因，保留登录窗口

### [ChatFrame.java](src/client/ChatFrame.java)
- 聊天室主界面（BorderLayout 布局）
- NORTH: 用户信息标签 + 切换匿名按钮
- CENTER: 只读消息显示区（JTextArea in JScrollPane）
- EAST: 在线用户列表（JList + DefaultListModel）
- SOUTH: 输入框 + 发送按钮 + 提示标签
- 输入解析: `@@` 命令 / `@` 私聊 / 其他群聊
- `onMessage()`: 根据类型在 EDT 上更新界面
- `closeWindow()`: volatile closing 防重入保护

### [ChatClient.java](src/client/ChatClient.java)
- 客户端网络层，封装 Socket 通信
- `connect()`: 连接服务器，创建 UTF-8 流
- `login()`: 发送 LOGIN 并同步等待响应
- `sendBroadcast()`, `sendPrivate()`, `sendCommand()`: 发送各类消息
- `readMessage()`: 读取并解码一行消息

### [ClientReceiver.java](src/client/ClientReceiver.java)
- 后台接收线程，循环读取服务器消息
- 每条消息通过 `SwingUtilities.invokeLater` 分发到 ChatFrame
- 连接断开时通知 ChatFrame 并触发关闭

## 4. resources/users.txt

12 个用户，格式 `用户名 密码`:

```
zhangsan 123456
lisi 123456
wangwu 123456
zhaoliu 123456
sunqi 123456
zhouba 123456
wujiu 123456
zhengshi password
qianyi password
liuer password
testuser test123
admin admin123
```

支持 `#` 开头的注释行和空行（加载时自动跳过）。

## 5. logs 目录

- 自动创建，存放每日滚动日志
- 文件名格式: `chat_yyyy-MM-dd.log`
- 日志格式: `[yyyy-MM-dd HH:mm:ss] ACTION | user=username | ip=ip | detail`
- ACTION 类型: LOGIN_SUCCESS, LOGIN_FAIL, LOGOUT, system

## 6. docs 目录

- [gui_acceptance_test.md](docs/gui_acceptance_test.md): GUI 手动验收测试文档
- [final_file_list.md](docs/final_file_list.md): 本文件清单

## 7. 测试类

| 文件 | 类型 | 测试项数 | 说明 |
|------|------|---------|------|
| TestMessage.java | 单元测试 | 10+ | Message encode/decode，含 pipe-in-content 边界情况 |
| TestServerCore.java | 单元测试 | 10+ | UserManager 和 LogManager |
| TestServerIntegration.java | 集成测试 | 27 | 全流程：登录/群聊/私聊/匿名/命令/USER_JOIN/USER_LEAVE |
| TestServerManual.md | 手册 | — | 使用 nc 手动测试服务器 |
| ClientManualTest.md | 手册 | — | 客户端手动测试说明 |
