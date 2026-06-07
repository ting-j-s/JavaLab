import common.Constants;
import server.LogManager;
import server.UserManager;

public class TestServerCore {

    public static void main(String[] args) {
        int passed = 0;
        int failed = 0;

        System.out.println("========== UserManager Tests ==========\n");

        // Test 1: create and load
        UserManager um = new UserManager();
        try {
            um.loadUsersFromFile(Constants.USER_FILE_PATH);
        } catch (Exception e) {
            System.out.println("FAIL: loadUsersFromFile threw " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }

        int count = um.getRegisteredUserCount();
        check("loadUsersFromFile - count >= 10", count >= 10, "count=" + count);

        // Test 2: authenticate correct
        boolean auth1 = um.authenticate("zhangsan", "123456");
        check("authenticate correct password", auth1, "zhangsan/123456");

        // Test 3: authenticate wrong password
        boolean auth2 = um.authenticate("zhangsan", "wrong");
        check("authenticate wrong password", !auth2, null);

        // Test 4: authenticate non-existent user
        boolean auth3 = um.authenticate("noone", "123456");
        check("authenticate non-existent user", !auth3, null);

        // Test 5: userExists
        boolean exists1 = um.userExists("zhangsan");
        check("userExists existing", exists1, null);

        boolean exists2 = um.userExists("ghost");
        check("userExists non-existing", !exists2, null);

        // Test 6: addOnlineUser
        boolean add1 = um.addOnlineUser("zhangsan", new Object());
        check("addOnlineUser success", add1, null);

        boolean online1 = um.isOnline("zhangsan");
        check("isOnline after add", online1, null);

        // Test 7: duplicate addOnlineUser
        boolean add2 = um.addOnlineUser("zhangsan", new Object());
        check("duplicate addOnlineUser should fail", !add2, null);

        // Test 8: addOnlineUser non-existent
        boolean add3 = um.addOnlineUser("ghost", new Object());
        check("addOnlineUser non-existent should fail", !add3, null);

        // Test 9: online user list
        um.addOnlineUser("lisi", new Object());
        um.addOnlineUser("wangwu", new Object());
        java.util.List<String> onlineList = um.getOnlineUserList();
        check("online user list size", onlineList.size() == 3, "size=" + onlineList.size());
        check("online user count", um.getOnlineUserCount() == 3, "count=" + um.getOnlineUserCount());
        System.out.println("  online users: " + onlineList);

        // Test 10: removeOnlineUser
        um.removeOnlineUser("lisi");
        check("removeOnlineUser", !um.isOnline("lisi") && um.getOnlineUserCount() == 2, null);

        // Test 11: all user list
        java.util.List<String> allList = um.getAllUserList();
        System.out.println("  registered users (" + allList.size() + "): " + allList);

        System.out.println("\n========== LogManager Tests ==========\n");

        // Test 12: create LogManager
        LogManager lm = new LogManager(Constants.LOG_DIR);

        // Test 13-16: write logs
        lm.logLoginSuccess("zhangsan", "127.0.0.1");
        System.out.println("  wrote LOGIN_SUCCESS");

        lm.logLoginFail("hacker", "192.168.1.100");
        System.out.println("  wrote LOGIN_FAIL");

        lm.logLogout("zhangsan", "127.0.0.1");
        System.out.println("  wrote LOGOUT");

        lm.logSystem("server startup test");
        System.out.println("  wrote SYSTEM");

        System.out.println("\n========== All tests complete ==========");
        System.out.println("Check logs/ directory for: chat_" + java.time.LocalDate.now() + ".log");
        System.out.println("File should contain: LOGIN_SUCCESS, LOGIN_FAIL, LOGOUT, SYSTEM");
    }

    private static int testCount = 0;

    private static void check(String label, boolean condition, String detail) {
        testCount++;
        if (condition) {
            System.out.println("  OK [" + testCount + "] " + label);
        } else {
            System.out.print("  FAIL [" + testCount + "] " + label);
            if (detail != null) {
                System.out.print(" — " + detail);
            }
            System.out.println();
            System.exit(1);
        }
    }
}
