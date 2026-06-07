package server;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UserManager {

    private final Map<String, String> registeredUsers = new ConcurrentHashMap<>();
    private final Map<String, Object> onlineUsers = new ConcurrentHashMap<>();

    public void loadUsersFromFile(String filePath) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(filePath), "UTF-8"))) {
            String line;
            int lineNo = 0;
            while ((line = reader.readLine()) != null) {
                lineNo++;
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                String[] parts = line.split("\\s+");
                if (parts.length < 2) {
                    System.out.println("[WARN] skip malformed line " + lineNo + ": " + line);
                    continue;
                }
                registeredUsers.put(parts[0], parts[1]);
            }
        }
    }

    public boolean authenticate(String username, String password) {
        String stored = registeredUsers.get(username);
        return stored != null && stored.equals(password);
    }

    public boolean userExists(String username) {
        return registeredUsers.containsKey(username);
    }

    public boolean isOnline(String username) {
        return onlineUsers.containsKey(username);
    }

    public boolean addOnlineUser(String username, Object handler) {
        if (!userExists(username)) {
            return false;
        }
        return onlineUsers.putIfAbsent(username, handler) == null;
    }

    public void removeOnlineUser(String username) {
        onlineUsers.remove(username);
    }

    public Object getOnlineUser(String username) {
        return onlineUsers.get(username);
    }

    public List<String> getOnlineUserList() {
        List<String> list = new ArrayList<>(onlineUsers.keySet());
        Collections.sort(list);
        return list;
    }

    public List<String> getAllUserList() {
        List<String> list = new ArrayList<>(registeredUsers.keySet());
        Collections.sort(list);
        return list;
    }

    public int getRegisteredUserCount() {
        return registeredUsers.size();
    }

    public int getOnlineUserCount() {
        return onlineUsers.size();
    }
}
