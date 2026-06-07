# GUI 手动验收测试文档

## 1. 编译

```bash
javac -encoding UTF-8 -d out src/common/*.java src/server/*.java src/client/*.java
```

## 2. 启动服务器

打开终端 1：

```bash
java -cp out server.ChatServer
```

预期输出：
```
[INFO] loaded 12 registered users
[INFO] server started on port 8888
[CONSOLE] commands: list | listall | quit
```

## 3. 启动 5 个客户端

分别打开终端 2~6，每个终端执行：

```bash
java -cp out client.LoginFrame
```

## 4. 测试账号

| 账号 | 密码 |
|------|------|
| zhangsan | 123456 |
| lisi | 123456 |
| wangwu | 123456 |
| zhaoliu | 123456 |
| sunqi | 123456 |

## 5. 逐项测试

### 操作指南

- 普通文本输入 → 群聊消息
- `@用户名 消息` → 私聊消息（如 `@lisi 你好，这是私聊`）
- `@@list` → 查看在线用户
- `@@showanonymous` → 查看当前匿名状态
- `@@anonymous` → 切换匿名/实名模式
- `@@quit` → 退出客户端

### 测试表

| 编号 | 测试项 | 操作 | 预期结果 | 实际结果 | 是否通过 |
|------|--------|------|----------|----------|----------|
| 1 | 服务器启动并加载用户文件 | 运行 `java -cp out server.ChatServer` | 显示 loaded 12 registered users | | |
| 2 | 客户端正确登录 | 输入 zhangsan/123456 登录 | 进入聊天室主界面 | | |
| 3 | 错误密码登录失败 | 输入 zhangsan/wrongpass | 显示"登录失败: invalid username or password"，保留登录窗口 | | |
| 4 | 同一用户重复登录 | zhangsan 已登录时再登录 zhangsan | 显示"user already online" | | |
| 5 | 5 个客户端同时在线 | 5 个账号分别登录 | 所有用户都进入聊天室，右侧在线列表显示 5 人 | | |
| 6 | 群聊消息所有用户可见 | zhangsan 输入"大家好" | 所有 5 个客户端都显示 `[群聊] zhangsan: 大家好` | | |
| 7 | 私聊消息仅目标用户可见 | zhangsan 输入 `@lisi 你好，这是私聊` | lisi 看到 `[私聊] zhangsan → 我: 你好，这是私聊`，zhangsan 看到确认消息，其他人看不到 | | |
| 8 | @@list 显示在线用户 | 任一客户端输入 `@@list` | 右侧在线列表刷新，显示所有在线用户 | | |
| 9 | @@showanonymous 显示当前模式 | 输入 `@@showanonymous` | 显示 `current chat mode: real name` | | |
| 10 | @@anonymous 切换匿名模式 | 输入 `@@anonymous` | 显示 `switched to anonymous chat`，顶部状态栏变为"模式: 匿名" | | |
| 11 | 匿名群聊 from 显示 anonymous | 匿名模式下输入"我现在是匿名消息" | 其他用户看到 `[群聊] anonymous: 我现在是匿名消息` | | |
| 12 | 匿名私聊 from 显示 anonymous | 匿名模式下输入 `@wangwu 这是一条匿名私聊` | wangwu 看到 `[私聊] anonymous → 我: 这是一条匿名私聊` | | |
| 13 | @@quit 退出客户端 | 输入 `@@quit` | 显示"bye"，窗口关闭。其他用户看到 USER_LEAVE 系统消息 | | |
| 14 | 关闭窗口退出客户端 | 点击窗口右上角 X | 窗口关闭，服务器不崩溃。其他用户看到 USER_LEAVE | | |
| 15 | 服务器控制台 list | 在服务器终端输入 `list` | 显示当前在线用户列表和数量 | | |
| 16 | 服务器控制台 listall | 在服务器终端输入 `listall` | 显示全部 12 个注册用户 | | |
| 17 | 服务器控制台 quit | 在服务器终端输入 `quit` | 服务器关闭，所有客户端显示"server shutting down"并断开 | | |
| 18 | logs 目录生成日志文件 | 查看 `logs/` 目录 | 存在 `chat_yyyy-MM-dd.log` 文件 | | |
| 19 | 日志包含 LOGIN_SUCCESS | 打开日志文件 | 包含 `LOGIN_SUCCESS` 记录，含用户名、IP、时间 | | |
| 20 | 日志包含 LOGIN_FAIL | 测试错误密码后查看日志 | 包含 `LOGIN_FAIL` 记录，含用户名、IP、时间 | | |
| 21 | 日志包含 LOGOUT | 客户端退出后查看日志 | 包含 `LOGOUT` 记录，含用户名、IP、时间 | | |

## 6. 补充测试项

| 编号 | 测试项 | 操作 | 预期结果 |
|------|--------|------|----------|
| 22 | 匿名切回实名 | 匿名模式下再次执行 `@@anonymous` | 显示 `switched to real name chat`，顶部状态栏变为"模式: 实名" |
| 23 | 用户上线通知 | 新用户登录 | 所有已在线用户收到 `USER_JOIN` 消息，在线列表自动刷新 |
| 24 | 用户下线通知 | 已有用户退出 | 其他在线用户收到 `USER_LEAVE` 消息，在线列表自动刷新 |
| 25 | 私聊格式错误提示 | 输入 `@lisihello`（无空格） | 显示 `私聊格式：@用户名 消息内容` |
| 26 | 私聊空用户名 | 输入 `@ hello`（@后紧跟空格） | 显示 `私聊格式：@用户名 消息内容` |
| 27 | 点击"切换匿名"按钮 | 点击顶部的"切换匿名"按钮 | 与输入 @@anonymous 效果相同 |
| 28 | 关闭窗口服务器不崩溃 | 连续关闭多个客户端窗口 | 服务器正常运行，不输出异常堆栈 |
