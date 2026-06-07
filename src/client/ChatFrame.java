package client;

import common.Message;
import common.MessageType;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class ChatFrame extends JFrame {

    private final ChatClient client;
    private ClientReceiver receiver;

    private JLabel userInfoLabel;
    private JTextArea chatArea;
    private DefaultListModel<String> onlineUserModel;
    private JTextField inputField;

    private boolean isAnonymous;
    private volatile boolean closing;

    public ChatFrame(ChatClient client) {
        this.client = client;
        initUI();
        startReceiver();
        // Auto-request online user list
        client.sendCommand("list");
    }

    private void initUI() {
        setTitle("聊天室 - " + client.getUsername());
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(700, 500);
        setMinimumSize(new Dimension(500, 350));

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                client.sendCommand("quit");
                closeWindow();
            }
        });

        setLayout(new BorderLayout());

        // === NORTH: user info + anonymous button ===
        JPanel northPanel = new JPanel(new BorderLayout());
        userInfoLabel = new JLabel("当前用户: " + client.getUsername() + " | 模式: 实名");
        northPanel.add(userInfoLabel, BorderLayout.CENTER);

        JButton anonymousButton = new JButton("切换匿名");
        anonymousButton.addActionListener(e -> client.sendCommand("anonymous"));
        northPanel.add(anonymousButton, BorderLayout.EAST);
        add(northPanel, BorderLayout.NORTH);

        // === CENTER: chat area ===
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        add(new JScrollPane(chatArea), BorderLayout.CENTER);

        // === EAST: online user list ===
        JPanel eastPanel = new JPanel(new BorderLayout());
        eastPanel.setPreferredSize(new Dimension(130, 0));
        eastPanel.add(new JLabel("在线用户"), BorderLayout.NORTH);

        onlineUserModel = new DefaultListModel<>();
        JList<String> onlineUserList = new JList<>(onlineUserModel);
        eastPanel.add(new JScrollPane(onlineUserList), BorderLayout.CENTER);
        add(eastPanel, BorderLayout.EAST);

        // === SOUTH: input area ===
        JPanel southPanel = new JPanel(new BorderLayout());

        JLabel tipLabel = new JLabel("直接输入为群聊；@用户名 内容 为私聊；@@list / @@anonymous / @@showanonymous / @@quit 为命令");
        southPanel.add(tipLabel, BorderLayout.NORTH);

        JPanel inputPanel = new JPanel(new BorderLayout());
        inputField = new JTextField();
        inputField.addActionListener(e -> handleSendInput());
        inputPanel.add(inputField, BorderLayout.CENTER);

        JButton sendButton = new JButton("发送");
        sendButton.addActionListener(e -> handleSendInput());
        inputPanel.add(sendButton, BorderLayout.EAST);

        southPanel.add(inputPanel, BorderLayout.SOUTH);
        add(southPanel, BorderLayout.SOUTH);

        setLocationRelativeTo(null);
    }

    private void startReceiver() {
        receiver = new ClientReceiver(client, this);
        new Thread(receiver, "ClientReceiver-" + client.getUsername()).start();
    }

    private void handleSendInput() {
        String text = inputField.getText().trim();
        if (text.isEmpty()) {
            return;
        }

        if (text.startsWith("@@")) {
            String command = text.substring(2);
            if ("quit".equals(command)) {
                client.sendCommand("quit");
                appendSystemMessage("正在退出聊天室...");
                closeWindow();
                return;
            }
            client.sendCommand(command);
        } else if (text.startsWith("@")) {
            int spaceIdx = text.indexOf(' ');
            if (spaceIdx <= 1 || spaceIdx == text.length() - 1) {
                appendSystemMessage("私聊格式：@用户名 消息内容");
            } else {
                String target = text.substring(1, spaceIdx);
                String content = text.substring(spaceIdx + 1);
                client.sendPrivate(target, content);
            }
        } else {
            client.sendBroadcast(text);
        }

        inputField.setText("");
    }

    public void onMessage(Message msg) {
        MessageType type = msg.getType();
        switch (type) {
            case BROADCAST:
                appendChatMessage("[群聊] " + msg.getFrom() + ": " + msg.getContent());
                break;
            case PRIVATE:
                appendChatMessage("[私聊] " + msg.getFrom() + " -> 我: " + msg.getContent());
                break;
            case SYSTEM:
                handleSystemMessage(msg.getContent());
                break;
            case USER_LIST:
                updateUserList(msg.getContent());
                break;
            case USER_JOIN:
                appendSystemMessage(msg.getContent());
                client.sendCommand("list");
                break;
            case USER_LEAVE:
                appendSystemMessage(msg.getContent());
                client.sendCommand("list");
                break;
            case ERROR:
                appendSystemMessage("[错误] " + msg.getContent());
                break;
            case LOGIN_SUCCESS:
            case LOGIN_FAIL:
                appendSystemMessage("[" + type + "] " + msg.getContent());
                break;
            default:
                break;
        }
    }

    private void handleSystemMessage(String content) {
        if (content == null) {
            return;
        }
        appendSystemMessage(content);

        if (content.contains("switched to anonymous") || content.contains("current chat mode: anonymous")) {
            isAnonymous = true;
            updateUserInfoLabel();
        } else if (content.contains("switched to real name") || content.contains("current chat mode: real name")) {
            isAnonymous = false;
            updateUserInfoLabel();
        } else if (content.contains("bye")) {
            closeWindow();
        }
    }

    private void updateUserInfoLabel() {
        String mode = isAnonymous ? "匿名" : "实名";
        userInfoLabel.setText("当前用户: " + client.getUsername() + " | 模式: " + mode);
    }

    private void updateUserList(String content) {
        onlineUserModel.clear();
        if (content != null && !content.isEmpty() && !"(none)".equals(content)) {
            String[] users = content.split(", ");
            for (String user : users) {
                if (!user.isEmpty()) {
                    onlineUserModel.addElement(user.trim());
                }
            }
        }
    }

    private void appendChatMessage(String text) {
        chatArea.append(text + "\n");
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }

    public void appendSystemMessage(String text) {
        chatArea.append("[系统] " + text + "\n");
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }

    public void closeWindow() {
        if (closing) return;
        closing = true;
        if (receiver != null) {
            receiver.stopReceiver();
        }
        client.close();
        dispose();
        System.exit(0);
    }
}
