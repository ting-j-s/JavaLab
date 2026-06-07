# 服务器端手动测试说明

## 编译

```bash
javac -encoding UTF-8 -d out src/common/*.java src/server/*.java
```

## 启动服务器

```bash
java -cp out server.ChatServer
```

服务器将在 8888 端口监听，启动时加载 `resources/users.txt`。

## 使用 nc 模拟客户端

### 终端 1：服务器

```bash
java -cp out server.ChatServer
```

### 终端 2：用户 zhangsan

```bash
nc 127.0.0.1 8888
```

输入以下消息（每行一条）：

```
LOGIN|zhangsan||123456|0
BROADCAST|zhangsan|ALL|hello everyone|0
SYSTEM|zhangsan||list|0
SYSTEM|zhangsan||showanonymous|0
SYSTEM|zhangsan||anonymous|0
BROADCAST|zhangsan|ALL|im anonymous now|0
SYSTEM|zhangsan||anonymous|0
SYSTEM|zhangsan||quit|0
```

### 终端 3：用户 lisi

```bash
nc 127.0.0.1 8888
```

```
LOGIN|lisi||123456|0
PRIVATE|lisi|zhangsan|hello secret|0
SYSTEM|lisi||quit|0
```

### 终端 4：错误密码测试

```bash
nc 127.0.0.1 8888
```

```
LOGIN|zhangsan||wrongpass|0
LOGIN|zhangsan||123456|0
SYSTEM|zhangsan||quit|0
```

## 服务器控制台命令

在服务器终端输入：

| 命令 | 作用 |
|------|------|
| `list` | 列出当前在线用户 |
| `listall` | 列出全部注册用户 |
| `quit` | 关闭服务器 |

## 预期现象

| 测试项 | 期望结果 |
|--------|----------|
| 正确登录 | 返回 `LOGIN_SUCCESS` |
| 错误密码 | 返回 `LOGIN_FAIL` 可重试 |
| 重复登录 | 返回 `LOGIN_FAIL` user already online |
| 群聊消息 | 所有在线用户都能收到 |
| 私聊消息 | 仅目标用户收到，发送者收到确认 |
| 匿名切换 | `anonymous` 后 from 变为 `anonymous` |
| 匿名切回 | 再次 `anonymous` 后 from 恢复真实用户名 |
| `@@list` → `SYSTEM\|...\|list` | 返回在线用户列表 |
| `@@quit` → `SYSTEM\|...\|quit` | 返回 bye 并断开 |
| 用户上线 | 其他用户收到 `USER_JOIN` |
| 用户下线 | 其他用户收到 `USER_LEAVE` |
| 日志 | `logs/chat_日期.log` 记录所有登录/登出/失败 |

## 自动化测试

也可用集成测试类验证：

```bash
# 终端 1: 启动服务器
java -cp out server.ChatServer

# 终端 2: 运行测试
java -cp out TestServerIntegration
```
