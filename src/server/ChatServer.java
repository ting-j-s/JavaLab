package server;

import common.Constants;
import common.Message;
import common.MessageType;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ChatServer {

    private ServerSocket serverSocket;
    private volatile boolean running = true;
    private final UserManager userManager = new UserManager();
    private final LogManager logManager = new LogManager(Constants.LOG_DIR);
    private final Set<ClientHandler> handlers = ConcurrentHashMap.newKeySet();

    public static void main(String[] args) {
        new ChatServer().start();
    }

    public void start() {
        try {
            userManager.loadUsersFromFile(Constants.USER_FILE_PATH);
            System.out.println("[INFO] loaded " + userManager.getRegisteredUserCount() + " registered users");
        } catch (IOException e) {
            System.err.println("[ERROR] failed to load user file: " + e.getMessage());
            return;
        }

        Thread consoleThread = new Thread(new ServerConsole(this), "ServerConsole");
        consoleThread.setDaemon(true);
        consoleThread.start();

        try {
            serverSocket = new ServerSocket(Constants.SERVER_PORT);
            System.out.println("[INFO] server started on port " + Constants.SERVER_PORT);
        } catch (IOException e) {
            System.err.println("[ERROR] failed to start server: " + e.getMessage());
            return;
        }

        logManager.logSystem("server started");

        while (running) {
            try {
                Socket socket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(socket, this);
                handlers.add(handler);
                new Thread(handler, "Client-" + socket.getInetAddress()).start();
            } catch (IOException e) {
                if (running) {
                    System.err.println("[ERROR] accept failed: " + e.getMessage());
                }
            }
        }
    }

    public void broadcast(Message message) {
        for (ClientHandler handler : handlers) {
            if (handler.isLoggedIn()) {
                handler.sendMessage(message);
            }
        }
    }

    public void broadcastExcept(Message message, ClientHandler except) {
        for (ClientHandler handler : handlers) {
            if (handler != except && handler.isLoggedIn()) {
                handler.sendMessage(message);
            }
        }
    }

    public boolean sendToUser(String username, Message message) {
        Object obj = userManager.getOnlineUser(username);
        if (obj instanceof ClientHandler) {
            ClientHandler handler = (ClientHandler) obj;
            handler.sendMessage(message);
            return true;
        }
        return false;
    }

    public void removeHandler(ClientHandler handler) {
        handlers.remove(handler);
    }

    public UserManager getUserManager() { return userManager; }

    public LogManager getLogManager() { return logManager; }

    public boolean isRunning() { return running; }

    public void shutdown() {
        running = false;
        Message shutdownMsg = new Message(MessageType.SYSTEM, "SERVER", null, "server shutting down");
        for (ClientHandler handler : handlers) {
            if (handler.isLoggedIn()) {
                handler.sendMessage(shutdownMsg);
            }
            handler.close();
        }
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("[ERROR] closing server socket: " + e.getMessage());
        }
        logManager.logSystem("server shutdown");
        System.out.println("[INFO] server shutdown complete");
    }
}
