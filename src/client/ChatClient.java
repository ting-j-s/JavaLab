package client;

import common.Constants;
import common.Message;
import common.MessageType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

public class ChatClient {

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String username;
    private volatile boolean connected;

    public ChatClient() {
    }

    public boolean connect() {
        try {
            socket = new Socket(Constants.SERVER_HOST, Constants.SERVER_PORT);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), Constants.CHARSET));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), Constants.CHARSET), true);
            connected = true;
            return true;
        } catch (IOException e) {
            System.err.println("[CLIENT] failed to connect to server: " + e.getMessage());
            return false;
        }
    }

    public Message login(String username, String password) {
        Message request = new Message(MessageType.LOGIN, username, "", password);
        sendMessage(request);

        Message response = readMessage();
        if (response == null) {
            return new Message(MessageType.ERROR, "CLIENT", null, "server disconnected");
        }
        if (response.getType() == MessageType.LOGIN_SUCCESS) {
            this.username = username;
        }
        return response;
    }

    public void sendBroadcast(String content) {
        sendMessage(new Message(MessageType.BROADCAST, username, "ALL", content));
    }

    public void sendPrivate(String target, String content) {
        sendMessage(new Message(MessageType.PRIVATE, username, target, content));
    }

    public void sendCommand(String command) {
        sendMessage(new Message(MessageType.SYSTEM, username, "", command));
    }

    public Message readMessage() {
        try {
            String line = in.readLine();
            if (line == null) {
                return null;
            }
            return Message.decode(line);
        } catch (IOException e) {
            return null;
        } catch (Exception e) {
            return new Message(MessageType.ERROR, "CLIENT", null, "parse error: " + e.getMessage());
        }
    }

    public void sendMessage(Message msg) {
        if (out != null) {
            out.println(msg.encode());
            out.flush();
        }
    }

    public void close() {
        connected = false;
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException ignored) {
        }
    }

    public String getUsername() {
        return username;
    }

    public boolean isConnected() {
        return connected && socket != null && !socket.isClosed();
    }
}
