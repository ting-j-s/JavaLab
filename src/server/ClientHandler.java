package server;

import common.Message;
import common.MessageType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private final ChatServer server;
    private BufferedReader in;
    private PrintWriter out;
    private volatile boolean running = true;
    private volatile boolean loggedIn = false;
    private boolean anonymousMode = false;
    private String username;
    private String clientIp;
    private boolean cleanedUp = false;

    public ClientHandler(Socket socket, ChatServer server) {
        this.socket = socket;
        this.server = server;
        this.clientIp = socket.getInetAddress().getHostAddress();
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);

            String line;
            while (running && (line = in.readLine()) != null) {
                Message msg;
                try {
                    msg = Message.decode(line);
                } catch (Exception e) {
                    sendError("invalid message format");
                    continue;
                }

                if (!loggedIn) {
                    if (msg.getType() == MessageType.LOGIN) {
                        handleLogin(msg);
                    } else {
                        sendError("please login first");
                    }
                } else {
                    switch (msg.getType()) {
                        case BROADCAST:
                            handleBroadcast(msg);
                            break;
                        case PRIVATE:
                            handlePrivate(msg);
                            break;
                        case SYSTEM:
                            handleSystem(msg);
                            break;
                        default:
                            sendError("unexpected message type: " + msg.getType());
                    }
                }
            }
        } catch (IOException e) {
            // client disconnected
        } finally {
            cleanup();
        }
    }

    private void handleLogin(Message msg) {
        String user = msg.getFrom();
        String pass = msg.getContent();

        if (user == null || user.isEmpty() || pass == null || pass.isEmpty()) {
            sendMessage(new Message(MessageType.LOGIN_FAIL, "SERVER", null, "username and password required"));
            return;
        }

        if (!server.getUserManager().authenticate(user, pass)) {
            server.getLogManager().logLoginFail(user, clientIp);
            sendMessage(new Message(MessageType.LOGIN_FAIL, "SERVER", null, "invalid username or password"));
            return;
        }

        if (!server.getUserManager().addOnlineUser(user, this)) {
            sendMessage(new Message(MessageType.LOGIN_FAIL, "SERVER", null, "user already online"));
            return;
        }

        this.username = user;
        this.loggedIn = true;
        server.getLogManager().logLoginSuccess(username, clientIp);
        sendMessage(new Message(MessageType.LOGIN_SUCCESS, "SERVER", null, "login success"));
        server.broadcastExcept(
                new Message(MessageType.USER_JOIN, "SERVER", null, username + " joined the chat"),
                this);
        System.out.println("[INFO] " + username + " logged in from " + clientIp);
    }

    private void handleBroadcast(Message msg) {
        String displayName = anonymousMode ? "anonymous" : username;
        Message broadcast = new Message(MessageType.BROADCAST, displayName, "ALL", msg.getContent());
        server.broadcast(broadcast);
    }

    private void handlePrivate(Message msg) {
        String target = msg.getTo();
        if (target == null || target.isEmpty()) {
            sendError("private message requires target username");
            return;
        }
        String displayName = anonymousMode ? "anonymous" : username;
        Message privateMsg = new Message(MessageType.PRIVATE, displayName, target, msg.getContent());

        if (server.sendToUser(target, privateMsg)) {
            sendMessage(new Message(MessageType.SYSTEM, "SERVER", null,
                    "private message sent to " + target));
        } else {
            sendError("user " + target + " is not online");
        }
    }

    private void handleSystem(Message msg) {
        String content = msg.getContent();
        if (content == null) {
            sendError("empty command");
            return;
        }
        String cmd = content.trim().toLowerCase();

        switch (cmd) {
            case "list":
                String onlineList = String.join(", ", server.getUserManager().getOnlineUserList());
                sendMessage(new Message(MessageType.USER_LIST, "SERVER", null,
                        onlineList.isEmpty() ? "(none)" : onlineList));
                break;
            case "quit":
                sendMessage(new Message(MessageType.SYSTEM, "SERVER", null, "bye"));
                running = false;
                break;
            case "showanonymous":
                String mode = anonymousMode ? "anonymous" : "real name";
                sendMessage(new Message(MessageType.SYSTEM, "SERVER", null,
                        "current chat mode: " + mode));
                break;
            case "anonymous":
                anonymousMode = !anonymousMode;
                String newMode = anonymousMode ? "anonymous" : "real name";
                sendMessage(new Message(MessageType.SYSTEM, "SERVER", null,
                        "switched to " + newMode + " chat"));
                break;
            default:
                sendError("unknown command: " + cmd);
        }
    }

    private void sendError(String detail) {
        sendMessage(new Message(MessageType.ERROR, "SERVER", null, detail));
    }

    public void sendMessage(Message msg) {
        if (out != null) {
            out.println(msg.encode());
            out.flush();
        }
    }

    public boolean isLoggedIn() { return loggedIn; }

    public String getUsername() { return username; }

    public void close() {
        running = false;
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException ignored) {
        }
    }

    private synchronized void cleanup() {
        if (cleanedUp) return;
        cleanedUp = true;

        if (loggedIn) {
            server.getUserManager().removeOnlineUser(username);
            server.getLogManager().logLogout(username, clientIp);
            server.broadcastExcept(
                    new Message(MessageType.USER_LEAVE, "SERVER", null, username + " left the chat"),
                    this);
            System.out.println("[INFO] " + username + " disconnected");
        }
        server.removeHandler(this);
        close();
    }
}
