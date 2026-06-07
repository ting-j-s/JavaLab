# 客户端手动测试说明

## 编译

```bash
javac -encoding UTF-8 -d out src/common/*.java src/server/*.java src/client/*.java
```

## 启动服务器

```bash
java -cp out server.ChatServer
```

服务器将在 8888 端口监听，启动时加载 `resources/users.txt`。

## 启动客户端

```bash
java -cp out client.LoginFrame
```

## 多客户端测试

打开多个终端，每个终端分别运行：

```bash
java -cp out client.LoginFrame
```

## 测试账号

| 用户名 | 密码 |
|--------|------|
| zhangsan | 123456 |
| lisi | 123456 |
| wangwu | 123456 |
| zhaoliu | 123456 |
| admin | admin123 |

完整列表见 `resources/users.txt`。

## 测试项目

### 1. 正确登录
- 输入正确的用户名和密码
- 点击"登录"
- 期望：进入聊天室主界面

### 2. 错误密码
- 输入正确用户名但错误密码
- 点击"登录"
- 期望：登录窗口显示"登录失败: invalid username or password"，密码框清空，可重新输入

### 3. 重复登录
- 用同一账号在两个客户端同时登录
- 期望：第二个客户端显示"登录失败: user already online"

### 4. 群聊
- 在输入框输入普通文本，点击发送或按回车
- 期望：所有在线用户（包括自己）都能看到群聊消息
- 显示格式：[群聊] 用户名: 消息内容

### 5. 私聊
- 输入 `@lisi 你好` 格式的消息
- 期望：
  - 发送者看到：[系统] private message sent to lisi
  - 接收者看到：[私聊] 用户名 → 我: 你好
- 如果格式错误（如 `@lisi` 无内容），提示"私聊格式：@用户名 消息内容"

### 6. 查看在线用户 @@list
- 输入 `@@list`
- 期望：右侧在线用户列表刷新，显示当前所有在线用户

### 7. 查看匿名状态 @@showanonymous
- 输入 `@@showanonymous`
- 期望：[系统] current chat mode: real name

### 8. 切换匿名 @@anonymous
- 输入 `@@anonymous`
- 期望：[系统] switched to anonymous chat，顶部状态栏变为"模式: 匿名"
- 再次输入 `@@anonymous` 切回实名
- 期望：[系统] switched to real name chat

### 9. 匿名群聊
- 切换到匿名模式后发送群聊消息
- 期望：其他用户看到 from 为 "anonymous"

### 10. 匿名私聊
- 切换到匿名模式后发送私聊
- 期望：接收者看到 from 为 "anonymous"

### 11. 退出 @@quit
- 输入 `@@quit`
- 期望：[系统] bye，窗口关闭

### 12. 直接关闭窗口
- 点击窗口右上角关闭按钮
- 期望：窗口关闭，服务器不崩溃

### 13. 服务器控制台命令
在服务器终端输入：

| 命令 | 作用 |
|------|------|
| `list` | 列出当前在线用户 |
| `listall` | 列出全部注册用户 |
| `quit` | 关闭服务器 |

### 14. 日志检查

查看 `logs/chat_日期.log`，应包含所有登录和退出记录。

## 预期现象汇总

| 测试项 | 期望结果 |
|--------|----------|
| 正确登录 | 进入聊天室主界面 |
| 错误密码 | 登录窗口保留，显示失败原因 |
| 重复登录 | 显示"user already online" |
| 群聊消息 | 所有在线用户都能收到 |
| 私聊消息 | 仅目标用户收到，发送者收到确认 |
| @@list | 右侧在线用户列表刷新 |
| @@showanonymous | 显示当前模式 |
| @@anonymous | 切换匿名/实名，顶部状态栏更新 |
| 匿名群聊 | from 显示 anonymous |
| 匿名私聊 | from 显示 anonymous |
| @@quit | 显示 bye 并退出 |
| 关闭窗口 | 服务器不崩溃，其他用户收到 USER_LEAVE |
| 用户上线 | 其他用户收到 USER_JOIN，在线列表自动更新 |
| 用户下线 | 其他用户收到 USER_LEAVE，在线列表自动更新 |
| 日志 | logs/chat_日期.log 记录所有登录/登出 |
