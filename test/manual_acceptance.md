# 聊天室手动验收流程

## 一、编译

```bash
cd JavaLab
javac -encoding UTF-8 -d out src/common/*.java src/server/*.java src/client/*.java
```

---

## 二、服务器端验收

### 2.1 启动服务器

打开终端 1：

```bash
java -cp out server.ChatServer
```

**预期输出：**

```
[INFO] loaded 12 registered users
[INFO] server started on port 8888
[CONSOLE] commands: list | listall | quit
```

**验证点：** ① 正确加载 12 个用户 ② 端口 8888 已绑定 ③ 提示符出现

### 2.2 list 命令

在服务器终端输入 `list` 回车。

**预期输出：**

```
Online users (0): []
```

**验证点：** 无客户端在线时为空列表

### 2.3 listall 命令

在服务器终端输入 `listall` 回车。

**预期输出：**

```
Registered users (12): [admin, liuer, lisi, qianyi, sunqi, testuser, wangwu, wujiu, zhangsan, zhaoliu, zhengshi, zhouba]
```

**验证点：** 列出全部 12 个注册用户，按字母排序

### 2.4 未知命令处理

输入 `hello` 回车。

**预期输出：**

```
[CONSOLE] unknown command. supported: list, listall, quit
```

---

## 三、客户端验收（单客户端）

### 3.1 编译并启动

打开终端 2：

```bash
java -cp out client.LoginFrame
```

**预期：** 弹出"聊天室登录"窗口，包含用户名输入框、密码输入框、登录按钮。

### 3.2 错误密码登录

输入用户名 `zhangsan`，密码 `wrongpass`，点击登录。

**预期：** 显示"登录失败: invalid username or password"，密码框清空，登录窗口保留，可再次登录。

### 3.3 空用户名/密码

不输入任何内容，直接点登录。

**预期：** 显示"用户名和密码不能为空"。

### 3.4 正确登录

输入 `zhangsan` / `123456`，点击登录。

**预期：**

- 登录窗口关闭
- 弹出聊天室主界面，标题栏显示"聊天室 - zhangsan"
- 顶部显示"当前用户: zhangsan | 模式: 实名"
- 右侧"在线用户"列表包含 `zhangsan`
- 消息区显示 `[系统] login success`
- 服务器终端显示 `[INFO] zhangsan logged in from 127.0.0.1`

---

## 四、多客户端验收（核心功能）

保持终端 2 的客户端运行，再依次打开 4 个终端（3~6），各启动一个客户端。

### 4.1 登录 5 个用户

| 终端 | 用户名 | 密码 |
|------|--------|------|
| 2 | zhangsan | 123456 |
| 3 | lisi | 123456 |
| 4 | wangwu | 123456 |
| 5 | zhaoliu | 123456 |
| 6 | sunqi | 123456 |

每登录一个，**其他已在线客户端**的消息区都会显示 `[系统] 新用户 joined the chat`，右侧在线列表自动刷新。

### 4.2 验证在线用户列表

在任一客户端输入 `@@list` 回车。

**预期：** 右侧在线列表显示 5 个用户名。

在服务器终端输入 `list`。

**预期：** `Online users (5): [lisi, sunqi, wangwu, zhangsan, zhaoliu]`

### 4.3 群聊消息

在 zhangsan（终端 2）的输入框中输入 `大家好，我是张三`，按回车。

**预期：**

- zhangsan 自己看到：`[群聊] zhangsan: 大家好，我是张三`
- 其余 4 个客户端都看到同一条消息
- 消息发送者显示为 `zhangsan`（实名）

### 4.4 私聊消息

在 zhangsan（终端 2）输入 `@lisi 这是一条私密消息`。

**预期：**

- zhangsan 看到：`[系统] private message sent to lisi`
- **只有 lisi** 看到：`[私聊] zhangsan → 我: 这是一条私密消息`
- wangwu、zhaoliu、sunqi **看不到**这条消息

### 4.5 私聊格式错误

在 zhangsan 输入 `@wangwu`（无空格无内容）。

**预期：** `[系统] 私聊格式：@用户名 消息内容`

输入 `@ hello`（@后跟空格）。

**预期：** 同上提示。

输入 `@nobody 你好`（不存在的用户）。

**预期：** `[错误] user nobody is not online`

---

## 五、匿名聊天验收

### 5.1 查看当前模式

在 zhangsan（终端 2）输入 `@@showanonymous`。

**预期：** `[系统] current chat mode: real name`

### 5.2 切换匿名

输入 `@@anonymous`。

**预期：**

- `[系统] switched to anonymous chat`
- 顶部状态栏变为"模式: 匿名"

也可点击顶部的"切换匿名"按钮，效果相同。

### 5.3 匿名群聊

在匿名模式下，zhangsan 输入 `猜猜我是谁`。

**预期：** 所有其他客户端看到 `[群聊] anonymous: 猜猜我是谁`（发送者显示为 anonymous，不暴露 zhangsan）。

### 5.4 匿名私聊

zhangsan 输入 `@lisi 你猜不到我是谁`。

**预期：**

- lisi 看到：`[私聊] anonymous → 我: 你猜不到我是谁`
- 发送者身份被隐藏

### 5.5 切回实名

输入 `@@anonymous`。

**预期：** `[系统] switched to real name chat`，顶部状态栏恢复"模式: 实名"。

之后再发群聊消息，发送者恢复显示 `zhangsan`。

---

## 六、退出验收

### 6.1 @@quit 退出

在 sunqi（终端 6）输入 `@@quit`。

**预期：**

- sunqi 看到 `[系统] bye`，窗口关闭
- 其余 4 个客户端看到 `[系统] sunqi left the chat`
- 在线列表更新为 4 人

### 6.2 关闭窗口退出

在 zhaoliu（终端 5）点击窗口右上角 **X** 按钮。

**预期：**

- 窗口正常关闭，不报错
- 其余客户端看到 `[系统] zhaoliu left the chat`
- 服务器不崩溃

---

## 七、重复登录防护

在终端 5 重新启动一个客户端，用 **已在线的** `lisi` 登录。

**预期：** 显示"登录失败: user already online"，登录窗口保留。

---

## 八、服务器 quit 验收

在服务器终端（终端 1）输入 `quit`。

**预期：**

- 所有客户端显示 `[系统] server shutting down`
- 随后显示 `[系统] 与服务器的连接已断开`，窗口关闭
- 服务器终端显示 `[INFO] server shutdown complete`
- 服务器进程退出

---

## 九、日志验收

### 9.1 日志文件存在

```bash
ls logs/
```

**预期：** 存在 `chat_yyyy-MM-dd.log` 文件。

### 9.2 日志内容

```bash
cat logs/chat_$(date +%Y-%m-%d).log
```

**预期格式：**

```
[2026-06-07 21:30:01] SYSTEM        | user=-         | ip=-             | server started
[2026-06-07 21:30:15] LOGIN_FAIL    | user=zhangsan  | ip=127.0.0.1     | invalid username or password
[2026-06-07 21:30:20] LOGIN_FAIL    | user=<null>    | ip=127.0.0.1     | invalid username or password
[2026-06-07 21:30:25] LOGIN_SUCCESS | user=zhangsan  | ip=127.0.0.1     | login success
[2026-06-07 21:30:30] LOGIN_SUCCESS | user=lisi      | ip=127.0.0.1     | login success
[2026-06-07 21:30:35] LOGIN_FAIL    | user=lisi      | ip=127.0.0.1     | user already online
[2026-06-07 21:31:00] LOGOUT        | user=sunqi     | ip=127.0.0.1     | logout
[2026-06-07 21:31:30] LOGOUT        | user=zhaoliu   | ip=127.0.0.1     | logout
[2026-06-07 21:32:00] SYSTEM        | user=-         | ip=-             | server shutdown
```

**验证点：**

- 登录成功：含用户名、IP、时间
- 登录失败（密码错误 / 空凭证 / 重复登录）：含尝试的用户名、IP、时间
- 退出：含用户名、IP、时间
- 服务器启停事件

---

## 十、验收检查清单

| # | 测试项 | 通过 |
|---|--------|------|
| 1 | 服务端启动，加载 12 个用户 | ☐ |
| 2 | 服务端 `list` 命令 | ☐ |
| 3 | 服务端 `listall` 命令 | ☐ |
| 4 | 服务端 `quit` 关闭 | ☐ |
| 5 | 客户端启动，连接服务器 | ☐ |
| 6 | 正确用户名密码登录成功 | ☐ |
| 7 | 错误密码登录失败，可重试 | ☐ |
| 8 | 空用户名/密码提示错误 | ☐ |
| 9 | 同一用户重复登录被拒绝 | ☐ |
| 10 | 5 个客户端同时在线 | ☐ |
| 11 | 群聊消息所有人可见 | ☐ |
| 12 | 私聊消息仅目标用户可见 | ☐ |
| 13 | 私聊格式错误提示 | ☐ |
| 14 | 私聊不存在的用户提示错误 | ☐ |
| 15 | `@@list` 显示在线用户 | ☐ |
| 16 | `@@showanonymous` 显示当前模式 | ☐ |
| 17 | `@@anonymous` 切换匿名/实名 | ☐ |
| 18 | 匿名模式下 from 显示 anonymous | ☐ |
| 19 | 匿名私聊 from 显示 anonymous | ☐ |
| 20 | 切回实名后恢复正常显示 | ☐ |
| 21 | "切换匿名"按钮可用 | ☐ |
| 22 | `@@quit` 退出 | ☐ |
| 23 | 关闭窗口退出，服务器不崩溃 | ☐ |
| 24 | 用户上线通知（USER_JOIN） | ☐ |
| 25 | 用户下线通知（USER_LEAVE） | ☐ |
| 26 | 服务器 `quit` 通知所有客户端并关闭 | ☐ |
| 27 | 日志文件记录登录成功 | ☐ |
| 28 | 日志文件记录登录失败 | ☐ |
| 29 | 日志文件记录退出 | ☐ |
| 30 | 日志文件记录服务器启停 | ☐ |
