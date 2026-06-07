package server;

import java.util.Scanner;

public class ServerConsole implements Runnable {

    private final ChatServer server;

    public ServerConsole(ChatServer server) {
        this.server = server;
    }

    @Override
    public void run() {
        Scanner scanner = new Scanner(System.in, "UTF-8");
        System.out.println("[CONSOLE] commands: list | listall | quit");

        while (server.isRunning()) {
            String line;
            try {
                line = scanner.nextLine();
            } catch (Exception e) {
                break;
            }
            String cmd = line.trim().toLowerCase();

            switch (cmd) {
                case "list":
                    System.out.println("Online users (" + server.getUserManager().getOnlineUserCount() + "): "
                            + server.getUserManager().getOnlineUserList());
                    break;
                case "listall":
                    System.out.println("Registered users (" + server.getUserManager().getRegisteredUserCount() + "): "
                            + server.getUserManager().getAllUserList());
                    break;
                case "quit":
                    System.out.println("[CONSOLE] shutting down...");
                    server.shutdown();
                    return;
                default:
                    if (!cmd.isEmpty()) {
                        System.out.println("[CONSOLE] unknown command. supported: list, listall, quit");
                    }
            }
        }
    }
}
