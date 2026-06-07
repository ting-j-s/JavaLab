package client;

import common.Message;

import javax.swing.SwingUtilities;

public class ClientReceiver implements Runnable {

    private final ChatClient client;
    private final ChatFrame chatFrame;
    private volatile boolean running = true;

    public ClientReceiver(ChatClient client, ChatFrame chatFrame) {
        this.client = client;
        this.chatFrame = chatFrame;
    }

    @Override
    public void run() {
        while (running && client.isConnected()) {
            Message msg = client.readMessage();
            if (msg == null) {
                SwingUtilities.invokeLater(() -> {
                    chatFrame.appendSystemMessage("与服务器的连接已断开");
                    chatFrame.closeWindow();
                });
                break;
            }
            SwingUtilities.invokeLater(() -> chatFrame.onMessage(msg));
        }
    }

    public void stopReceiver() {
        running = false;
    }
}
