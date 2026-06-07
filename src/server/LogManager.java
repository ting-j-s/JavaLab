package server;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LogManager {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final String logDir;

    public LogManager(String logDir) {
        this.logDir = logDir;
        try {
            Files.createDirectories(Paths.get(logDir));
        } catch (IOException e) {
            System.err.println("[ERROR] failed to create log directory: " + e.getMessage());
        }
    }

    public synchronized void logLoginSuccess(String username, String ip) {
        writeLog("LOGIN_SUCCESS", username, ip, "login success");
    }

    public synchronized void logLoginFail(String username, String ip) {
        writeLog("LOGIN_FAIL", username, ip, "invalid username or password");
    }

    public synchronized void logLogout(String username, String ip) {
        writeLog("LOGOUT", username, ip, "logout");
    }

    public synchronized void logSystem(String content) {
        writeLog("SYSTEM", "-", "-", content);
    }

    private synchronized void writeLog(String action, String username, String ip, String detail) {
        String time = LocalDateTime.now().format(TIME_FMT);
        String line = String.format("[%s] %-13s | user=%-10s | ip=%-15s | %s",
                time, action, username, ip, detail);

        String date = LocalDateTime.now().format(DATE_FMT);
        Path logFile = Paths.get(logDir, "chat_" + date + ".log");

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile.toFile(), true))) {
            writer.write(line);
            writer.newLine();
        } catch (IOException e) {
            System.err.println("[ERROR] failed to write log: " + e.getMessage());
        }
    }
}
