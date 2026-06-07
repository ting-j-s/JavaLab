package client;

import common.Message;
import common.MessageType;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class LoginFrame extends JFrame {

    private ChatClient client;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JLabel statusLabel;

    public LoginFrame() {
        initClient();
        initUI();
    }

    private void initClient() {
        client = new ChatClient();
        if (!client.connect()) {
            JOptionPane.showMessageDialog(null, "无法连接服务器 " + common.Constants.SERVER_HOST + ":" + common.Constants.SERVER_PORT,
                    "连接失败", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    private void initUI() {
        setTitle("聊天室登录");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                client.close();
                System.exit(0);
            }
        });

        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 10, 5, 10);
        c.fill = GridBagConstraints.HORIZONTAL;

        JLabel userLabel = new JLabel("用户名");
        c.gridx = 0;
        c.gridy = 0;
        add(userLabel, c);

        usernameField = new JTextField(15);
        c.gridx = 1;
        c.gridy = 0;
        add(usernameField, c);

        JLabel passLabel = new JLabel("密码");
        c.gridx = 0;
        c.gridy = 1;
        add(passLabel, c);

        passwordField = new JPasswordField(15);
        c.gridx = 1;
        c.gridy = 1;
        add(passwordField, c);

        loginButton = new JButton("登录");
        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 2;
        add(loginButton, c);

        statusLabel = new JLabel(" ");
        statusLabel.setForeground(java.awt.Color.RED);
        c.gridy = 3;
        add(statusLabel, c);

        loginButton.addActionListener(e -> doLogin());

        // Enter key triggers login
        passwordField.addActionListener(e -> doLogin());

        pack();
        setLocationRelativeTo(null);
    }

    private void doLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("用户名和密码不能为空");
            return;
        }

        loginButton.setEnabled(false);
        statusLabel.setText("正在登录...");
        statusLabel.setForeground(java.awt.Color.BLACK);

        new Thread(() -> {
            Message response = client.login(username, password);

            SwingUtilities.invokeLater(() -> {
                if (response.getType() == MessageType.LOGIN_SUCCESS) {
                    new ChatFrame(client).setVisible(true);
                    dispose();
                } else if (response.getType() == MessageType.LOGIN_FAIL) {
                    statusLabel.setText("登录失败: " + response.getContent());
                    statusLabel.setForeground(java.awt.Color.RED);
                    passwordField.setText("");
                    loginButton.setEnabled(true);
                } else {
                    statusLabel.setText("错误: " + (response.getContent() != null ? response.getContent() : "未知错误"));
                    statusLabel.setForeground(java.awt.Color.RED);
                    loginButton.setEnabled(true);
                }
            });
        }).start();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}
